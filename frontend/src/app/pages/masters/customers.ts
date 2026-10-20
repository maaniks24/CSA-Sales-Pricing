import { Component, inject } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Customer } from '../../core/models';
import { FieldDef, MasterCrud } from './master-crud';

@Component({
  selector: 'app-customers',
  imports: [MasterCrud],
  template: `<app-master-crud title="Customer" [fields]="fields" [api]="api.customers" [defaults]="defaults" />`,
})
export class Customers {
  api = inject(ApiService);
  fields: FieldDef[] = [
    { key: 'code', label: 'Code', type: 'text', required: true, column: true, width: 90 },
    { key: 'name', label: 'Customer name', type: 'text', required: true, column: true, wide: true },
    { key: 'city', label: 'City', type: 'text', column: true, width: 120 },
    { key: 'vertical', label: 'Vertical', type: 'text', column: true, width: 130 },
    { key: 'contactName', label: 'Contact person', type: 'text', column: true, width: 160 },
    { key: 'contactEmail', label: 'Contact email', type: 'text' },
    { key: 'contactPhone', label: 'Contact phone', type: 'text' },
    { key: 'assetCount', label: '# of assets', type: 'number', decimals: 0, min: 0, column: true, width: 110 },
    { key: 'branchCount', label: '# of branches', type: 'number', decimals: 0, min: 1, column: true, width: 120 },
    { key: 'discountPercent', label: 'Discount %', type: 'number', decimals: 2, max: 100, column: true, width: 110 },
    { key: 'address', label: 'Address (one line per row, printed on the document)', type: 'textarea' },
    { key: 'active', label: 'Active', type: 'bool', column: true, width: 80 },
  ];
  defaults = (): Customer => ({
    code: '', name: '', assetCount: 0, branchCount: 1, discountPercent: 0, active: true,
  });
}
