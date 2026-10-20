import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Customer, DashboardDto, Numbering, PricingConfig, ProposalDto, ProposalRequest, ProposalSummary, ServiceItem, Software,
} from './models';

const API = '/api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  dashboard(): Observable<DashboardDto> {
    return this.http.get<DashboardDto>(`${API}/dashboard`);
  }

  // masters ---------------------------------------------------------------
  customers = this.crud<Customer>('customers');
  software = this.crud<Software>('software');
  services = this.crud<ServiceItem>('services');

  config(): Observable<PricingConfig> {
    return this.http.get<PricingConfig>(`${API}/config`);
  }
  saveConfig(c: PricingConfig): Observable<PricingConfig> {
    return this.http.put<PricingConfig>(`${API}/config`, c);
  }

  numbering(): Observable<Numbering> {
    return this.http.get<Numbering>(`${API}/numbering`);
  }
  saveNumbering(n: Numbering): Observable<Numbering> {
    return this.http.put<Numbering>(`${API}/numbering`, n);
  }

  // proposals -------------------------------------------------------------
  proposals(): Observable<ProposalSummary[]> {
    return this.http.get<ProposalSummary[]>(`${API}/proposals`);
  }
  proposal(id: number): Observable<ProposalDto> {
    return this.http.get<ProposalDto>(`${API}/proposals/${id}`);
  }
  calculate(r: ProposalRequest): Observable<ProposalDto> {
    return this.http.post<ProposalDto>(`${API}/proposals/calculate`, r);
  }
  createProposal(r: ProposalRequest): Observable<ProposalDto> {
    return this.http.post<ProposalDto>(`${API}/proposals`, r);
  }
  updateProposal(id: number, r: ProposalRequest): Observable<ProposalDto> {
    return this.http.put<ProposalDto>(`${API}/proposals/${id}`, r);
  }
  deleteProposal(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/proposals/${id}`);
  }
  generatePo(id: number): Observable<HttpResponse<Blob>> {
    return this.http.post(`${API}/proposals/${id}/generate-po`, null, { observe: 'response', responseType: 'blob' });
  }

  private crud<T extends { id?: number }>(path: string) {
    const url = `${API}/${path}`;
    return {
      list: () => this.http.get<T[]>(url),
      save: (item: T) => (item.id ? this.http.put<T>(`${url}/${item.id}`, item) : this.http.post<T>(url, item)),
      remove: (id: number) => this.http.delete<void>(`${url}/${id}`),
    };
  }
}
