import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { InputsModule } from '@progress/kendo-angular-inputs';
import { ApiService } from '../../core/api.service';
import { PricingConfig } from '../../core/models';
import { Notify } from '../../core/notify.service';

interface Row { key: string; label: string; decimals?: number; max?: number }

@Component({
  selector: 'app-settings',
  imports: [FormsModule, ButtonsModule, InputsModule],
  template: `
    <div class="page-head">
      <h1>Pricing Settings</h1>
      <button kendoButton themeColor="primary" (click)="save()" [disabled]="!cfg">Save settings</button>
    </div>
    <p class="muted">These defaults are copied into every new proposal and can still be changed per proposal.
      Saved proposals keep the values they were created with.</p>
    @if (cfg) {
      @for (g of groups; track g.title) {
        <section class="card">
          <h2>{{ g.title }}</h2>
          <div class="grid-2">
            @for (r of g.rows; track r.key) {
              <div class="field">
                <label>{{ r.label }}</label>
                <kendo-numerictextbox [(ngModel)]="cfg[g.target][r.key]" [min]="0" [max]="r.max ?? 1e12"
                  [decimals]="r.decimals ?? 2" [format]="(r.decimals ?? 2) === 0 ? 'n0' : 'n2'" [spinners]="false"></kendo-numerictextbox>
              </div>
            }
          </div>
        </section>
      }
    }
  `,
})
export class Settings implements OnInit {
  private api = inject(ApiService);
  private notify = inject(Notify);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  cfg: any = null;

  groups: { title: string; target: 'params' | 'terms'; rows: Row[] }[] = [
    {
      title: 'Rate card - per person per day', target: 'params',
      rows: [
        { key: 'trackingRoom', label: 'Tracking - room expense' },
        { key: 'trackingFood', label: 'Tracking - food expense' },
        { key: 'trackingSalary', label: 'Tracking - salary (CPD-PP)' },
        { key: 'dataAuditSalary', label: 'Data audit - salary (CPD-PP)' },
        { key: 'srExecRoom', label: 'Sr executive - room expense' },
        { key: 'srExecFood', label: 'Sr executive - food expense' },
        { key: 'srExecSalary', label: 'Sr executive - salary' },
      ],
    },
    {
      title: 'Loadings', target: 'params',
      rows: [
        { key: 'costPerBranch', label: 'Cost per branch' },
        { key: 'travelCost', label: 'Travel / local conveyance' },
        { key: 'overheadPercent', label: 'Overheads %' },
        { key: 'profitPercent', label: 'Profit %' },
      ],
    },
    {
      title: 'Document terms', target: 'terms',
      rows: [
        { key: 'gstPercent', label: 'GST %', max: 100 },
        { key: 'validityDays', label: 'Proposal validity (days)', decimals: 0 },
        { key: 'commencementDays', label: 'Project start (days after PO)', decimals: 0 },
        { key: 'trackingTeamSize', label: 'Tracking team size', decimals: 0 },
        { key: 'dataTeamSize', label: 'Data audit team size', decimals: 0 },
        { key: 'pay1', label: 'Payment on PO %', decimals: 0, max: 100 },
        { key: 'pay2', label: 'Payment on physical verification %', decimals: 0, max: 100 },
        { key: 'pay3', label: 'Payment on completion %', decimals: 0, max: 100 },
      ],
    },
  ];

  ngOnInit() {
    this.api.config().subscribe({ next: (c: PricingConfig) => (this.cfg = c), error: (e) => this.notify.error(e) });
  }

  save() {
    const t = this.cfg.terms;
    if (t.pay1 + t.pay2 + t.pay3 !== 100) {
      this.notify.error({ message: 'Milestone payments must add up to 100%' });
      return;
    }
    this.api.saveConfig(this.cfg).subscribe({
      next: () => this.notify.success('Settings saved'),
      error: (e) => this.notify.error(e),
    });
  }
}
