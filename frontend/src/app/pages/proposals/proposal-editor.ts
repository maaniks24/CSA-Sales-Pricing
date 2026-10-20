import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { DateInputsModule } from '@progress/kendo-angular-dateinputs';
import { DropDownsModule } from '@progress/kendo-angular-dropdowns';
import { InputsModule } from '@progress/kendo-angular-inputs';
import { Subject, debounceTime, forkJoin, switchMap, of, catchError } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { saveBlob } from '../../core/blob';
import {
  Customer, DocTerms, PricingParams, ProposalDto, ProposalRequest, ServiceItem, ServiceLineDto, Software,
} from '../../core/models';
import { Notify } from '../../core/notify.service';

interface SwLine { softwareId: number | null; assetsCovered: number | null; }
interface SvLine { serviceId: number; code: string; name: string; inScope: boolean; assetsPerDay: number; }

@Component({
  selector: 'app-proposal-editor',
  imports: [FormsModule, RouterLink, DecimalPipe, ButtonsModule, DateInputsModule, DropDownsModule, InputsModule],
  templateUrl: './proposal-editor.html',
  styleUrl: './proposal-editor.scss',
})
export class ProposalEditor implements OnInit {
  /** route parameter :id (bound through withComponentInputBinding) */
  id = input<string>();

  private api = inject(ApiService);
  private notify = inject(Notify);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  customers = signal<Customer[]>([]);
  customerFilter = signal('');
  shownCustomers = computed(() => {
    const f = this.customerFilter().toLowerCase();
    return f ? this.customers().filter((c) => (c.name + ' ' + c.code).toLowerCase().includes(f)) : this.customers();
  });
  softwareMaster = signal<Software[]>([]);
  serviceMaster = signal<ServiceItem[]>([]);

  // ---- form state ----
  customerId: number | null = null;
  contactName = '';
  proposalDate: Date = new Date();
  assetCount = 0;
  branchCount = 1;
  discountPercent = 0;
  srExecDays = 0;
  notes = '';
  params!: PricingParams;
  terms!: DocTerms;
  software: SwLine[] = [];
  services: SvLine[] = [];

  // ---- state ----
  loaded = signal(false);
  result = signal<ProposalDto | null>(null);
  saved = signal<ProposalDto | null>(null);
  dirty = signal(false);
  busy = signal(false);
  calcError = signal('');

  readonly totals = computed(() => this.result()?.totals);
  /** Generate PO needs a saved proposal without pending edits. */
  readonly canGenerate = computed(() => !!this.saved()?.id && !this.dirty() && !this.busy());
  private recalc$ = new Subject<void>();

  get isNew() {
    return !this.id();
  }

  ngOnInit() {
    this.recalc$
      .pipe(
        debounceTime(250),
        switchMap(() => {
          const req = this.buildRequest();
          if (!req) { return of(null); }
          return this.api.calculate(req).pipe(
            catchError((e) => { this.calcError.set(e.error?.message ?? 'Calculation failed'); return of(null); }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        if (r) { this.calcError.set(''); this.result.set(r); }
      });

    const existing = this.id() ? this.api.proposal(+this.id()!) : of(null);
    forkJoin({
      customers: this.api.customers.list(),
      software: this.api.software.list(),
      services: this.api.services.list(),
      config: this.api.config(),
      proposal: existing,
    }).subscribe({
      next: ({ customers, software, services, config, proposal }) => {
        this.customers.set(customers.filter((c) => c.active || c.id === proposal?.customerId));
        this.softwareMaster.set(software.filter((s) => s.active));
        this.serviceMaster.set(services.filter((s) => s.active));
        this.params = { ...config.params };
        this.terms = { ...config.terms };
        if (proposal) { this.load(proposal); } else { this.initServices(); }
        this.loaded.set(true);
        this.recalc();
        this.dirty.set(false);
      },
      error: (e) => this.notify.error(e),
    });
  }

  // ---- loading ----
  private initServices(saved: ServiceLineDto[] = []) {
    const byId = new Map(saved.map((s) => [s.serviceId, s]));
    this.services = this.serviceMaster().map((m) => {
      const s = byId.get(m.id!);
      return {
        serviceId: m.id!, code: m.code, name: m.name,
        inScope: s ? s.inScope : saved.length ? false : m.defaultInScope,
        assetsPerDay: s ? s.assetsPerDay : m.assetsPerDay,
      };
    });
  }

  private load(p: ProposalDto) {
    this.saved.set(p);
    this.customerId = p.customerId;
    this.contactName = p.contactName ?? '';
    this.proposalDate = new Date(p.proposalDate + 'T00:00:00');
    this.assetCount = p.assetCount;
    this.branchCount = p.branchCount;
    this.discountPercent = p.discountPercent;
    this.srExecDays = p.srExecDays;
    this.notes = p.notes ?? '';
    this.params = { ...p.params };
    this.terms = { ...p.terms };
    this.software = p.software.map((s) => ({ softwareId: s.softwareId, assetsCovered: s.assetsCovered }));
    this.initServices(p.services);
  }

  // ---- user interaction ----
  onCustomer(id: number | null) {
    const c = this.customers().find((x) => x.id === id);
    if (c) {
      this.contactName = c.contactName ?? '';
      this.assetCount = c.assetCount;
      this.branchCount = c.branchCount;
      this.discountPercent = c.discountPercent;
      // software lines follow the customer's asset count again
      this.software.forEach((s) => (s.assetsCovered = null));
    }
    this.changed();
  }

  addSoftware() {
    this.software.push({ softwareId: null, assetsCovered: null });
    this.dirty.set(true);
  }

  removeSoftware(i: number) {
    this.software.splice(i, 1);
    this.changed();
  }

  changed() {
    this.dirty.set(true);
    this.recalc();
  }

  recalc() {
    this.recalc$.next();
  }

  lineFor(serviceId: number) {
    return this.result()?.services.find((l) => l.serviceId === serviceId);
  }

  softwareLine(i: number) {
    const id = this.software[i].softwareId;
    return this.result()?.software.filter((l) => l.softwareId === id)[this.software.slice(0, i).filter((s) => s.softwareId === id).length];
  }

  // ---- persistence ----
  private fmtDate(d: Date) {
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${day}`;
  }

  /** Returns null while the form is not complete enough to calculate. */
  private buildRequest(): ProposalRequest | null {
    if (!this.customerId || !this.params) { return null; }
    return {
      customerId: this.customerId,
      contactName: this.contactName,
      proposalDate: this.fmtDate(this.proposalDate ?? new Date()),
      assetCount: this.assetCount ?? 0,
      branchCount: this.branchCount ?? 1,
      srExecDays: this.srExecDays ?? 0,
      discountPercent: this.discountPercent ?? 0,
      notes: this.notes,
      params: this.params,
      terms: this.terms,
      software: this.software.filter((s) => s.softwareId).map((s) => ({ softwareId: s.softwareId!, assetsCovered: s.assetsCovered })),
      services: this.services.map((s) => ({ serviceId: s.serviceId, inScope: s.inScope, assetsPerDay: s.assetsPerDay })),
    };
  }

  save() {
    const req = this.buildRequest();
    if (!req) { this.notify.error({ message: 'Select a customer first' }); return; }
    this.busy.set(true);
    const id = this.saved()?.id;
    const call = id ? this.api.updateProposal(id, req) : this.api.createProposal(req);
    call.subscribe({
      next: (p) => {
        this.busy.set(false);
        this.saved.set(p);
        this.result.set(p);
        this.dirty.set(false);
        this.notify.success(`Saved ${p.refNo}`);
        if (!id) { this.router.navigate(['/proposals', p.id], { replaceUrl: true }); }
      },
      error: (e) => { this.busy.set(false); this.notify.error(e); },
    });
  }

  generatePo() {
    const id = this.saved()?.id;
    if (!id) { return; }
    this.busy.set(true);
    this.api.generatePo(id).subscribe({
      next: (res) => {
        saveBlob(res, 'proposal.docx');
        this.busy.set(false);
        this.notify.success('PO document generated');
        this.api.proposal(id).subscribe((p) => this.saved.set(p));
      },
      error: (e) => { this.busy.set(false); this.notify.error(e); },
    });
  }

  get title() {
    const s = this.saved();
    return s ? `Proposal ${s.refNo}` : 'New Proposal';
  }

  get hasServices() {
    return this.services.some((s) => s.inScope);
  }
}
