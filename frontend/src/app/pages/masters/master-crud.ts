import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { DialogModule } from '@progress/kendo-angular-dialog';
import { DropDownsModule } from '@progress/kendo-angular-dropdowns';
import { GridModule } from '@progress/kendo-angular-grid';
import { InputsModule } from '@progress/kendo-angular-inputs';
import { Observable } from 'rxjs';
import { Notify } from '../../core/notify.service';

export interface FieldDef {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'bool' | 'select';
  required?: boolean;
  min?: number;
  max?: number;
  decimals?: number;
  options?: { value: string; label: string }[];
  /** show as grid column */
  column?: boolean;
  width?: number;
  wide?: boolean;
}

export interface CrudApi<T> {
  list: () => Observable<T[]>;
  save: (item: T) => Observable<T>;
  remove: (id: number) => Observable<void>;
}

/** Generic master data screen: Kendo grid + dialog form generated from the field definitions. */
@Component({
  selector: 'app-master-crud',
  imports: [FormsModule, DecimalPipe, GridModule, DialogModule, ButtonsModule, InputsModule, DropDownsModule],
  template: `
    <div class="page-head">
      <h1>{{ title() }}</h1>
      <button kendoButton themeColor="primary" (click)="add()">+ Add</button>
    </div>
    <kendo-grid [data]="rows()" [loading]="loading()" [pageable]="{ pageSizes: [10, 25, 50] }" [pageSize]="10"
      [skip]="skip" (pageChange)="skip = $event.skip" [sortable]="true" [filterable]="'row'">
      @for (f of columns(); track f.key) {
        <kendo-grid-column [field]="f.key" [title]="f.label" [width]="f.width ?? 0"
          [class]="f.type === 'number' ? 'num' : ''" [headerClass]="f.type === 'number' ? 'num' : ''"
          [filter]="f.type === 'number' ? 'numeric' : f.type === 'bool' ? 'boolean' : 'text'">
          <ng-template kendoGridCellTemplate let-r>
            @if (f.type === 'bool') { {{ r[f.key] ? 'Yes' : 'No' }} }
            @else if (f.type === 'number') { {{ r[f.key] | number: '1.0-2' }} }
            @else if (f.type === 'select') { {{ optionLabel(f, r[f.key]) }} }
            @else { {{ r[f.key] }} }
          </ng-template>
        </kendo-grid-column>
      }
      <kendo-grid-column title="" [width]="150" [filterable]="false">
        <ng-template kendoGridCellTemplate let-r>
          <button kendoButton size="small" (click)="edit(r)">Edit</button>
          <button kendoButton size="small" fillMode="flat" themeColor="error" (click)="confirmDelete = r">Delete</button>
        </ng-template>
      </kendo-grid-column>
    </kendo-grid>

    @if (editing) {
      <kendo-dialog [title]="(editing.id ? 'Edit ' : 'Add ') + title()" (close)="editing = null" [width]="720" [minWidth]="300">
        <div class="grid-2 form">
          @for (f of fields(); track f.key) {
            <div class="field" [class.wide]="f.wide || f.type === 'textarea'">
              <label>{{ f.label }}@if (f.required) { <span class="error-text"> *</span>}</label>
              @switch (f.type) {
                @case ('text') { <kendo-textbox [(ngModel)]="editing[f.key]"></kendo-textbox> }
                @case ('textarea') { <kendo-textarea [(ngModel)]="editing[f.key]" [rows]="3"></kendo-textarea> }
                @case ('number') {
                  <kendo-numerictextbox [(ngModel)]="editing[f.key]" [min]="f.min ?? 0" [max]="f.max ?? 1e12"
                    [decimals]="f.decimals ?? 2" [format]="(f.decimals ?? 2) === 0 ? 'n0' : 'n2'" [spinners]="false"></kendo-numerictextbox>
                }
                @case ('bool') { <kendo-switch [(ngModel)]="editing[f.key]" onLabel="Yes" offLabel="No"></kendo-switch> }
                @case ('select') {
                  <kendo-dropdownlist [data]="f.options ?? []" textField="label" valueField="value" [valuePrimitive]="true"
                    [(ngModel)]="editing[f.key]"></kendo-dropdownlist>
                }
              }
            </div>
          }
        </div>
        @if (formError) { <p class="error-text">{{ formError }}</p> }
        <kendo-dialog-actions>
          <button kendoButton (click)="editing = null">Cancel</button>
          <button kendoButton themeColor="primary" (click)="save()">Save</button>
        </kendo-dialog-actions>
      </kendo-dialog>
    }

    @if (confirmDelete) {
      <kendo-dialog title="Delete" (close)="confirmDelete = null" [width]="420" [minWidth]="300">
        <p>Delete <b>{{ confirmDelete.name }}</b>? If it is used in proposals, mark it inactive instead.</p>
        <kendo-dialog-actions>
          <button kendoButton (click)="confirmDelete = null">Cancel</button>
          <button kendoButton themeColor="error" (click)="remove()">Delete</button>
        </kendo-dialog-actions>
      </kendo-dialog>
    }
  `,
})
export class MasterCrud<T extends { id?: number }> implements OnInit {
  title = input.required<string>();
  fields = input.required<FieldDef[]>();
  api = input.required<CrudApi<T>>();
  defaults = input.required<() => T>();

  private notify = inject(Notify);
  rows = signal<T[]>([]);
  loading = signal(true);
  skip = 0;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editing: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  confirmDelete: any = null;
  formError = '';

  columns = () => this.fields().filter((f) => f.column);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.api().list().subscribe({
      next: (r) => { this.rows.set(r); this.loading.set(false); },
      error: (e) => { this.loading.set(false); this.notify.error(e); },
    });
  }

  optionLabel(f: FieldDef, v: string) {
    return f.options?.find((o) => o.value === v)?.label ?? v;
  }

  add() {
    this.formError = '';
    this.editing = { ...this.defaults()() };
  }

  edit(r: T) {
    this.formError = '';
    this.editing = { ...r };
  }

  save() {
    const missing = this.fields().filter((f) => f.required && (this.editing[f.key] === null || this.editing[f.key] === undefined || this.editing[f.key] === ''));
    if (missing.length) {
      this.formError = 'Please fill: ' + missing.map((f) => f.label).join(', ');
      return;
    }
    this.api().save(this.editing).subscribe({
      next: () => { this.editing = null; this.notify.success('Saved'); this.load(); },
      error: (e) => { this.formError = e.error?.message ?? 'Save failed'; },
    });
  }

  remove() {
    const r = this.confirmDelete;
    this.confirmDelete = null;
    this.api().remove(r.id).subscribe({
      next: () => { this.notify.success('Deleted'); this.load(); },
      error: (e) => this.notify.error(e),
    });
  }
}
