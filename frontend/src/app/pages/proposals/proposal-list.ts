import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { DialogModule } from '@progress/kendo-angular-dialog';
import { GridModule } from '@progress/kendo-angular-grid';
import { ApiService } from '../../core/api.service';
import { saveBlob } from '../../core/blob';
import { ProposalSummary } from '../../core/models';
import { Notify } from '../../core/notify.service';

@Component({
  selector: 'app-proposal-list',
  imports: [RouterLink, GridModule, ButtonsModule, DialogModule, DecimalPipe],
  template: `
    <div class="page-head">
      <h1>Proposals</h1>
      <a routerLink="/proposals/new" class="k-button k-button-solid k-button-solid-primary k-button-md k-rounded-md">
        <span class="k-button-text">+ New Proposal</span></a>
    </div>
    <kendo-grid [data]="rows()" [pageable]="{ pageSizes: [10, 25, 50] }" [pageSize]="10" [skip]="skip"
      (pageChange)="skip = $event.skip" [sortable]="true" [filterable]="'menu'" [loading]="loading()">
      <kendo-grid-column field="refNo" title="Ref No" [width]="220">
        <ng-template kendoGridCellTemplate let-r><a [routerLink]="['/proposals', r.id]">{{ r.refNo }}</a></ng-template>
      </kendo-grid-column>
      <kendo-grid-column field="customerName" title="Customer"></kendo-grid-column>
      <kendo-grid-column field="proposalDate" title="Date" [width]="120"></kendo-grid-column>
      <kendo-grid-column field="poNo" title="PO No" [width]="190"></kendo-grid-column>
      <kendo-grid-column field="status" title="Status" [width]="130">
        <ng-template kendoGridCellTemplate let-r>
          <span class="chip" [class.po]="r.status === 'PO_GENERATED'" [class.saved]="r.status === 'SAVED'">
            {{ r.status === 'PO_GENERATED' ? 'PO generated' : 'Saved' }}</span>
        </ng-template>
      </kendo-grid-column>
      <kendo-grid-column field="grandTotal" title="Total (incl. GST)" [width]="160" class="num" headerClass="num">
        <ng-template kendoGridCellTemplate let-r>{{ r.grandTotal | number: '1.2-2' }}</ng-template>
      </kendo-grid-column>
      <kendo-grid-column title="" [width]="210" [sticky]="false">
        <ng-template kendoGridCellTemplate let-r>
          <button kendoButton size="small" (click)="download(r)">Generate PO</button>
          <button kendoButton size="small" fillMode="flat" themeColor="error" (click)="confirmDelete = r">Delete</button>
        </ng-template>
      </kendo-grid-column>
    </kendo-grid>

    @if (confirmDelete) {
      <kendo-dialog title="Delete proposal" (close)="confirmDelete = null" [minWidth]="300" [width]="420">
        <p>Delete proposal <b>{{ confirmDelete.refNo }}</b>? This cannot be undone.</p>
        <kendo-dialog-actions>
          <button kendoButton (click)="confirmDelete = null">Cancel</button>
          <button kendoButton themeColor="error" (click)="remove()">Delete</button>
        </kendo-dialog-actions>
      </kendo-dialog>
    }
  `,
})
export class ProposalList implements OnInit {
  private api = inject(ApiService);
  private notify = inject(Notify);
  rows = signal<ProposalSummary[]>([]);
  loading = signal(true);
  skip = 0;
  confirmDelete: ProposalSummary | null = null;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.api.proposals().subscribe({
      next: (r) => { this.rows.set(r); this.loading.set(false); },
      error: (e) => { this.loading.set(false); this.notify.error(e); },
    });
  }

  download(r: ProposalSummary) {
    this.api.generatePo(r.id).subscribe({
      next: (res) => { saveBlob(res, 'proposal.docx'); this.notify.success('PO document generated'); this.load(); },
      error: (e) => this.notify.error(e),
    });
  }

  remove() {
    const r = this.confirmDelete!;
    this.confirmDelete = null;
    this.api.deleteProposal(r.id).subscribe({
      next: () => { this.notify.success('Proposal deleted'); this.load(); },
      error: (e) => this.notify.error(e),
    });
  }
}
