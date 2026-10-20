import { Component, inject } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Software } from '../../core/models';
import { FieldDef, MasterCrud } from './master-crud';

@Component({
  selector: 'app-software-master',
  imports: [MasterCrud],
  template: `<app-master-crud title="Software" [fields]="fields" [api]="api.software" [defaults]="defaults" />`,
})
export class SoftwareMaster {
  api = inject(ApiService);
  fields: FieldDef[] = [
    { key: 'code', label: 'Code', type: 'text', required: true, column: true, width: 120 },
    { key: 'name', label: 'Software name', type: 'text', required: true, column: true, wide: true },
    { key: 'price', label: 'Subscription price', type: 'number', required: true, column: true, width: 150 },
    { key: 'assetBlock', label: 'Price covers # assets', type: 'number', decimals: 0, min: 1, required: true, column: true, width: 160 },
    { key: 'implementationCharge', label: 'Implementation (one-time)', type: 'number', column: true, width: 180 },
    { key: 'description', label: 'Description', type: 'textarea' },
    { key: 'scopeText', label: 'Scope text in cost table ({assets} = covered assets)', type: 'textarea' },
    { key: 'implementationText', label: 'Implementation text in cost table', type: 'textarea' },
    { key: 'active', label: 'Active', type: 'bool', column: true, width: 80 },
  ];
  defaults = (): Software => ({
    code: '', name: '', price: 0, assetBlock: 1000, implementationCharge: 0, active: true,
    scopeText: 'SaaS with 1-year subscription cost\nFor about {assets} assets',
  });
}
