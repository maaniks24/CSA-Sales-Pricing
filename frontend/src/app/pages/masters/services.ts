import { Component, inject } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { ServiceItem } from '../../core/models';
import { FieldDef, MasterCrud } from './master-crud';

@Component({
  selector: 'app-services-master',
  imports: [MasterCrud],
  template: `<app-master-crud title="Service" [fields]="fields" [api]="api.services" [defaults]="defaults" />`,
})
export class ServicesMaster {
  api = inject(ApiService);
  fields: FieldDef[] = [
    { key: 'sequenceNo', label: '#', type: 'number', decimals: 0, column: true, width: 70 },
    { key: 'code', label: 'Code', type: 'text', required: true, column: true, width: 90 },
    { key: 'name', label: 'Phase / service', type: 'text', required: true, column: true, wide: true },
    { key: 'assetsPerDay', label: 'PPD-PP (assets / person / day)', type: 'number', required: true, min: 0.01, column: true, width: 190 },
    {
      key: 'resourceType', label: 'Resource (rate card)', type: 'select', column: true, width: 150,
      options: [
        { value: 'TRACKING', label: 'Tracking' },
        { value: 'DATA_AUDIT', label: 'Data audit' },
        { value: 'SR_EXECUTIVE', label: 'Sr executive' },
      ],
    },
    { key: 'includeRoom', label: 'Room exp.', type: 'bool', column: true, width: 100 },
    { key: 'includeFood', label: 'Food exp.', type: 'bool', column: true, width: 100 },
    { key: 'defaultInScope', label: 'In scope by default', type: 'bool', column: true, width: 150 },
    { key: 'activity', label: 'Activity text in the timeline table', type: 'textarea' },
    { key: 'active', label: 'Active', type: 'bool', column: true, width: 80 },
  ];
  defaults = (): ServiceItem => ({
    code: '', name: '', sequenceNo: 10, assetsPerDay: 100, resourceType: 'TRACKING',
    includeRoom: false, includeFood: false, defaultInScope: true, active: true,
  });
}
