import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChartsModule } from '@progress/kendo-angular-charts';
import { GridModule } from '@progress/kendo-angular-grid';
import { ApiService } from '../../core/api.service';
import { DashboardDto } from '../../core/models';
import { Notify } from '../../core/notify.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, ChartsModule, GridModule, DecimalPipe, CurrencyPipe],
  template: `
    <div class="page-head"><h1>Dashboard</h1></div>
    @if (d(); as d) {
      <div class="kpis">
        <div class="kpi"><span>Customers</span><b>{{ d.customers }}</b></div>
        <div class="kpi"><span>Software</span><b>{{ d.software }}</b></div>
        <div class="kpi"><span>Services</span><b>{{ d.services }}</b></div>
        <div class="kpi"><span>Proposals</span><b>{{ d.proposals }}</b></div>
        <div class="kpi"><span>PO generated</span><b>{{ d.poGenerated }}</b></div>
        <div class="kpi wide"><span>Total proposal value (incl. GST)</span><b>{{ d.totalValue | currency: 'INR' : 'symbol' : '1.0-0' }}</b></div>
        <div class="kpi wide"><span>Value with PO generated</span><b>{{ d.poValue | currency: 'INR' : 'symbol' : '1.0-0' }}</b></div>
      </div>

      <div class="charts">
        <div class="card">
          <h2>Proposal value - last 6 months</h2>
          <kendo-chart [style.height.px]="260">
            <kendo-chart-legend [visible]="false"></kendo-chart-legend>
            <kendo-chart-category-axis>
              <kendo-chart-category-axis-item [categories]="months()"></kendo-chart-category-axis-item>
            </kendo-chart-category-axis>
            <kendo-chart-value-axis>
              <kendo-chart-value-axis-item [labels]="labels"></kendo-chart-value-axis-item>
            </kendo-chart-value-axis>
            <kendo-chart-series>
              <kendo-chart-series-item type="column" [data]="monthAmounts()" color="#1f6fb2" name="Value"></kendo-chart-series-item>
            </kendo-chart-series>
            <kendo-chart-tooltip format="{0:n0}"></kendo-chart-tooltip>
          </kendo-chart>
        </div>
        <div class="card">
          <h2>By status</h2>
          <kendo-chart [style.height.px]="260">
            <kendo-chart-legend position="bottom"></kendo-chart-legend>
            <kendo-chart-series>
              <kendo-chart-series-item type="donut" [data]="statusData()" field="count" categoryField="label"
                colorField="color"></kendo-chart-series-item>
            </kendo-chart-series>
          </kendo-chart>
        </div>
        <div class="card">
          <h2>Top customers by value</h2>
          <kendo-chart [style.height.px]="260">
            <kendo-chart-legend [visible]="false"></kendo-chart-legend>
            <kendo-chart-category-axis>
              <kendo-chart-category-axis-item [categories]="customers()"></kendo-chart-category-axis-item>
            </kendo-chart-category-axis>
            <kendo-chart-value-axis>
              <kendo-chart-value-axis-item [labels]="labels"></kendo-chart-value-axis-item>
            </kendo-chart-value-axis>
            <kendo-chart-series>
              <kendo-chart-series-item type="bar" [data]="customerAmounts()" color="#2a9d8f" name="Value"></kendo-chart-series-item>
            </kendo-chart-series>
            <kendo-chart-tooltip format="{0:n0}"></kendo-chart-tooltip>
          </kendo-chart>
        </div>
      </div>

      <div class="card">
        <h2>Recent proposals</h2>
        <kendo-grid [data]="d.recent">
          <kendo-grid-column field="refNo" title="Ref No" [width]="200">
            <ng-template kendoGridCellTemplate let-r><a [routerLink]="['/proposals', r.id]">{{ r.refNo }}</a></ng-template>
          </kendo-grid-column>
          <kendo-grid-column field="customerName" title="Customer"></kendo-grid-column>
          <kendo-grid-column field="proposalDate" title="Date" [width]="120"></kendo-grid-column>
          <kendo-grid-column title="Status" [width]="140">
            <ng-template kendoGridCellTemplate let-r>
              <span class="chip" [class.po]="r.status === 'PO_GENERATED'" [class.saved]="r.status === 'SAVED'">
                {{ r.status === 'PO_GENERATED' ? 'PO generated' : 'Saved' }}</span>
            </ng-template>
          </kendo-grid-column>
          <kendo-grid-column title="Total" [width]="150" class="num" headerClass="num">
            <ng-template kendoGridCellTemplate let-r>{{ r.grandTotal | number: '1.2-2' }}</ng-template>
          </kendo-grid-column>
        </kendo-grid>
      </div>
    }
  `,
  styles: `
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 16px; }
    .kpi { background: #fff; border: 1px solid #dde3ea; border-radius: 6px; padding: 12px 16px; display: flex; flex-direction: column; }
    .kpi span { font-size: .8rem; color: #5b6b7c; } .kpi b { font-size: 1.5rem; margin-top: 4px; }
    .kpi.wide { grid-column: span 2; }
    .charts { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 16px; }
  `,
})
export class Dashboard implements OnInit {
  private api = inject(ApiService);
  private notify = inject(Notify);
  d = signal<DashboardDto | null>(null);

  private compact = new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 });
  labels = { content: (e: { value: number }) => this.compact.format(e.value) };

  months = () => this.d()?.monthly.map((m) => m.month) ?? [];
  monthAmounts = () => this.d()?.monthly.map((m) => m.amount) ?? [];
  customers = () => this.d()?.topCustomers.map((c) => c.customer) ?? [];
  customerAmounts = () => this.d()?.topCustomers.map((c) => c.amount) ?? [];
  statusData = () =>
    (this.d()?.byStatus ?? []).map((s) => ({
      label: s.status === 'PO_GENERATED' ? 'PO generated' : 'Saved',
      count: s.count,
      color: s.status === 'PO_GENERATED' ? '#2a9d8f' : '#e9b44c',
    }));

  ngOnInit() {
    this.api.dashboard().subscribe({ next: (d) => this.d.set(d), error: (e) => this.notify.error(e) });
  }
}
