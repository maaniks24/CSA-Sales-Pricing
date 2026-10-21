% Proposal Module - Integration Guide
% How to add the Proposal / PO feature of CSA Sales Pricing to an existing sales application
% Version 1.0 - 21 Oct 2026

# 1. Purpose and scope

The standalone **CSA Sales Pricing** application contains four things:

| Part | Owned by the existing (host) application? | What to do |
|---|---|---|
| Customer master | Yes, already exists | Do **not** copy. Expose it to the proposal module (section 4 / 5). |
| Software master | Yes, already exists | Do **not** copy. Only a few pricing attributes are added (section 4.2). |
| Service master | Yes, already exists | Do **not** copy. Only a few pricing attributes are added (section 4.2). |
| **Proposal module** (proposal screen, price calculation, reference numbering, Pricing Settings, Word PO generation, dashboard) | No | **Move this into the host application.** |

Everything the proposal module needs from the masters is a short, fixed list of fields (section 6 "Master data contract").
The module stores only the **ids** of host customers / software / services.

## 1.1 Fast path (summary)

| # | Step | Who | Typical effort |
|---|---|---|---|
| 1 | Check compatibility (Java, Spring Boot, Angular, Kendo, PostgreSQL) - section 3 | Both | 0.5 h |
| 2 | Run SQL scripts 01-03 on the host database - section 4 | DB / backend | 2-3 h |
| 3 | Copy backend packages, add dependencies and properties - section 5 | Backend | 3-4 h |
| 4 | Map the three master entities to the host data (3 `@Table` lines, or own entities) - section 5.4 | Backend | 1-2 h |
| 5 | Copy frontend files, dependencies and routes - section 7 | Frontend | 3-4 h |
| 6 | Add the pricing fields to the host's software / service master screens - section 8 (the proposal screen itself needs no change, it reads the lookup API) | Frontend | 2-3 h |
| 7 | Run the acceptance checks - section 9 | QA | 2 h |

About **2 working days** for a team of one backend and one frontend developer, when versions are compatible.

## 1.2 What was verified

The approach was tested with a simulated host database: differently named customer / software / service tables with
other column names, the three SQL scripts of this guide applied, Flyway disabled, the master REST endpoints switched
off (`csa.masters.enabled=false`) and the three entities pointed at the adapter views. Hibernate schema validation
passed, a proposal for a host customer was calculated and saved, the Word document was generated, and numbering worked.
It was **not** tested inside your real application - use section 9 for that.

# 2. Architecture (what talks to what)

```
Angular (host app)                         Spring Boot (host app)                    PostgreSQL (host DB)
-------------------                        ----------------------                    --------------------
Proposal list / editor  --/api/proposals-->  ProposalController                      proposal
Pricing Settings        --/api/config----->  SettingsController                      proposal_software
                        --/api/numbering-->                                          proposal_service
Dashboard (optional)    --/api/dashboard-->  ProposalsService, NumberingService      pricing_config
                                             PricingCalculator                       doc_sequence
Host Customer/Software/                      DocxTemplateEngine + Word templates          |
Service screens (existing)                   CsaCustomer / CsaSoftware / CsaServiceItem   | read only
   add pricing fields  --------------------> (entities mapped to host tables via views) --> host customer / software /
                                                                                          service tables (+ 2 side tables)
```

# 3. Compatibility check (do this first)

| Item | Standalone app uses | If the host differs |
|---|---|---|
| Java | 21 | Java 17 should work (change `java.version`; the module code uses no Java 21-only features - confirm with a build; the standalone project is only built and tested on 21). Java 8/11: not supported by Spring Boot 3. |
| Spring Boot | 3.5.x (Jakarta `jakarta.*`, Hibernate 6) | Boot 2.x (`javax.*`, Hibernate 5): replace `jakarta.` by `javax.` in the module and re-check the Hibernate mapping of embedded classes; plan an extra 0.5-1 day. |
| Database | PostgreSQL 16 | Other databases need the SQL scripts translated (types `numeric`, `boolean`, identity columns). |
| Build | Maven | Gradle: translate the dependencies in 5.2. |
| Lombok | used (`@Getter`, `@Setter`) | Add Lombok, or generate getters / setters. |
| Angular | 20 | **Minimum 18.1** (uses `@if/@for/@let` control flow and signal `input()`). Older: needs rewriting the templates to `*ngIf/*ngFor`. |
| Kendo UI for Angular | 25.3 (module style imports: `GridModule`, `InputsModule`, ...) | Use the Kendo version that matches the host's Angular version and keep **all** `@progress/kendo-angular-*` packages on the same version. See troubleshooting 10.1. |
| Standalone components | yes | If the host uses NgModules, declare/import the components in a feature module (they are standalone, so `imports: [...]` in the NgModule works). |

# 4. Database

Run the three scripts in `docs/integration/sql/` **in this order** with the host's own migration mechanism
(Flyway / Liquibase / DBA ticket). Do **not** reuse the standalone migrations `V1`-`V3` - they also create and seed the
customer / software / service tables.

## 4.1 Script 01 - proposal tables (`01-proposal-module-tables.sql`)

Creates `pricing_config`, `doc_sequence`, `proposal`, `proposal_software`, `proposal_service` and inserts the default row
of `pricing_config` (rates from the workbook, GST 18 %, numbering `LTPL/PRO`, `LTPL/PO`, 5 digits).

* Adjust before running: the default rates, prefixes and number size (they can also be changed later in the UI).
* **Id types.** `customer_id`, `software_id`, `service_id` are `bigint`. If the host uses UUID or varchar ids, change those
  three columns here **and** the id type of the matching master entity and `ProposalSoftware.software` /
  `ProposalService.service` / `Proposal.customer` joins in Java (section 5.4).
* **Foreign keys** to the host master tables are optional (commented at the end of the script). They are only possible
  when the master is a real table with a `bigint` id.
* If the host already has a table called `proposal`, `pricing_config` or `doc_sequence`, rename ours (script and
  `@Table(name = ...)` on the entity).

## 4.2 Script 02 - pricing attributes of software and services (`02-pricing-attributes.sql`)

The proposal price needs a few attributes that a plain software / service master normally does not have. They are kept in
two **side tables** so that the host's own tables are not changed:

| Table | Columns | Used for |
|---|---|---|
| `csa_software_pricing` | `software_id`, `scope_text`, `price`, `asset_block`, `implementation_charge`, `implementation_text` | Software price (price per block of assets, e.g. 90,000 per 3,000 assets, scaled proportionately), one-time implementation row, scope text in the Word cost table |
| `csa_service_pricing` | `service_id`, `activity`, `sequence_no`, `assets_per_day`, `resource_type`, `include_room`, `include_food`, `default_in_scope` | Service phase costing (effort = assets / assets per person-day), rate card used, room / food costs, order and timeline text |

Then load the rows for your existing software and services (the script has an example for the six workbook phases
FAR, PV, DC, DM, DR, TG). Rows for software / services without a side-table row are still usable - they fall back to
price 0 / 1 asset per day - so every new software or service needs a row (see 8).

*Alternative (A):* add the same columns to the host tables and select them directly in the view of script 03.

## 4.3 Script 03 - adapter views (`03-adapter-views.sql`)

Creates `csa_customer_v`, `csa_software_v`, `csa_service_v`, which present the host tables in the shape the proposal
module's three master entities expect. **This is the only place where the host's table and column names appear**:

1. In the script replace the example host tables `crm_customer`, `crm_software`, `crm_service` and the column on the right of every `as`.
2. Every column of the view must exist, with the declared type (use `null::varchar` / constants when the host has no equivalent). Hibernate validates the types at startup.
3. `active` must be a boolean (`true` = selectable in the proposal screen).
4. `address` is printed line by line in the Word document - build it with `concat_ws(E'\n', line1, line2, city_pin)`.
5. `contact_name` is the addressee ("Hi Vijay"), `discount_percent` the customer's default discount, `asset_count` and
   `branch_count` the defaults of the proposal.

## 4.4 Verify the database

```sql
select count(*) from csa_customer_v;                  -- = number of host customers
select id, name, assets_per_day, resource_type from csa_service_v order by sequence_no;
select * from pricing_config;                         -- one row, id = 1
select * from doc_sequence;                           -- empty until the first proposal is saved
```

## 4.5 Existing data and numbering

If the host already issues proposal numbers, set **Pricing Settings > Reference numbering** (prefix, digits, next number)
before the first proposal is created, or run
`insert into doc_sequence values ('PRO-2026-27', 43)` (last used number) for the current financial year
(`PRO` without year when the running number does not restart yearly).

# 5. Backend (Spring Boot)

## 5.1 Files to copy

Copy from `backend/src/main/java/com/levantare/pricing/` (keep the package `com.levantare.pricing`, or rename it
consistently with your IDE refactoring):

| Package | Files | Action |
|---|---|---|
| `domain` | `Proposal`, `ProposalSoftware`, `ProposalService`, `PricingConfig`, `PricingParams`, `DocTerms`, `NumberingConfig`, `DocSequence`, `ProposalStatus`, `ResourceType` | Copy |
| `domain` | `Customer`, `Software`, `ServiceItem` | Copy **then map** to the host data (5.4), or replace by your own entities |
| `repo` | `ProposalRepository`, `PricingConfigRepository`, `DocSequenceRepository` | Copy |
| `repo` | `CustomerRepository`, `SoftwareRepository`, `ServiceItemRepository` | Copy (read access) or replace by your repositories |
| `calc` | `PricingCalculator` | Copy (pure Java, no dependencies) |
| `doc` | `DocxTemplateEngine`, `Fmt`, `ProposalDocumentBuilder` | Copy |
| `service` | `ProposalsService`, `NumberingService`, `SequenceService`, `DashboardService`, `NotFoundException`, `BusinessException` | Copy |
| `web` | `ProposalController`, `SettingsController`, `LookupController`, `ApiExceptionHandler` | Copy |
| `web/dto` | `ProposalRequest`, `ProposalDto`, `ProposalSummary`, `NumberingDto`, `DashboardDto` | Copy |
| `web` | `MasterDataController` | **Skip** (the host already has master screens/APIs) |
| `web` | `CorsConfig`, `PricingApplication` | **Skip** (host has its own) |
| resources | `csa-templates/software-proposal.docx`, `csa-templates/services-proposal.docx` | Copy (kept out of `templates/`, which Boot reserves for Thymeleaf) |
| resources | `db/migration/*`, `application.yml` | **Skip** (use scripts 01-03 and the properties of 5.3) |

Entities are named `CsaProposal`, `CsaCustomer`, ... (`@Entity(name = "Csa...")`), so they do not clash with host
entities of the same simple class name. Table names are unchanged.

## 5.2 Dependencies (`pom.xml`)

```xml
<dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.4.1</version></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
<dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
<!-- web, data-jpa and the PostgreSQL driver are already in the host -->
```

## 5.3 Configuration

* Make sure the module is component-scanned and its entities / repositories are registered. If the host application
  class is in another package add:

```java
@SpringBootApplication(scanBasePackages = {"com.host", "com.levantare.pricing"})
@EntityScan({"com.host.domain", "com.levantare.pricing.domain"})
@EnableJpaRepositories({"com.host.repo", "com.levantare.pricing.repo"})
```

* `application.yml`: `csa.masters.enabled: false` (switches the standalone master CRUD off when the controller is copied
  by mistake), and keep `spring.jpa.hibernate.ddl-auto: validate` (or `none`) - never `update` on the shared database.
* JSON: the DTOs work with the default Jackson settings.
* **REST paths.** The module uses `/api/proposals`, `/api/config`, `/api/numbering`, `/api/dashboard`, `/api/lookups`. If they clash with
  host paths add a prefix, e.g. `@RequestMapping("/api/sales")` on the controllers - and change `API` in the
  Angular `api.service.ts` (section 7.3).
* **Security.** The standalone app has no authentication. Put the controllers behind the host's security
  (path rules or `@PreAuthorize`). Suggested roles: *sales* can create / edit proposals and generate the document;
  *admin* can change `PUT /api/config` and `PUT /api/numbering`. If the host records "created by", add `created_by` /
  `updated_by` to `proposal` (script and entity) and fill them in `ProposalsService.create/update`.
* `ApiExceptionHandler` is limited to the controllers of `com.levantare.pricing.web`, so it does not change error
  handling of host controllers. Errors come back as `{ "status": 409, "message": "..." }`; the Angular code reads `message`.

## 5.4 Connect the three master entities

**Recommended (no logic change): point the entities at the adapter views from script 03** - change one line in each:

```java
// domain/Customer.java      @Table(name = "customer")       ->  @Table(name = "csa_customer_v")
// domain/Software.java      @Table(name = "software")       ->  @Table(name = "csa_software_v")
// domain/ServiceItem.java   @Table(name = "service_master") ->  @Table(name = "csa_service_v")
```

The views are read-only for the module; the module never writes to the masters.

**Alternative: use the host's own entities.** Replace `CsaCustomer`, `CsaSoftware`, `CsaServiceItem` in
`Proposal`, `ProposalSoftware`, `ProposalService` with the host classes and make them provide the getters listed in
section 6, or put a small mapper in `ProposalsService.apply(...)` and `ProposalDocumentBuilder`.
Use this only if the views are not acceptable to the DBA.

## 5.5 Word templates

Two templates are in `csa-templates/`. The one with services (Apexon style) is used when the proposal has at least one
service in scope, the software one otherwise. To change wording, edit the .docx in Word and keep the `{{...}}`
placeholders. `docs/templates/Template-Placeholders.md` lists every placeholder and where its value comes from.
If the host's company details differ (address in footer, logo, signatory), change them in the Word file - header, footer
and signature block are plain template text.

## 5.6 Tests

Port `PricingCalculatorTest` as is (it reproduces the workbook: 441,562.50 for the sample). `ProposalFlowTest` needs the
standalone Flyway migrations and the master seed data - adapt it to the host's test database fixtures or keep it as a
reference for the API contract.

# 6. Master data contract

The proposal module reads these fields from the host masters. If a host field does not exist, supply a constant in the view.

## 6.1 Customer (`csa_customer_v`)

| Field | Type | Used for |
|---|---|---|
| `id` | bigint | Stored as `proposal.customer_id` |
| `name` | text | Addressee organisation in the document, lists, dashboard |
| `address` | text, `\n` separated | Address block of the document |
| `contact_name` | text | "Hi *contact*" and default of the proposal contact field |
| `asset_count`, `branch_count` | integer | Defaults of the proposal (editable per proposal) |
| `discount_percent` | numeric(5,2) | Default discount of the proposal (customer based discount) |
| `active` | boolean | Only active customers can be selected |
| `code`, `city`, `vertical`, `contact_email`, `contact_phone` | text | Screen filter / not printed (may be null) |

## 6.2 Software (`csa_software_v`)

| Field | Used for |
|---|---|
| `id`, `name`, `active` | Selection and the cost-table text |
| `price`, `asset_block` | Subscription price: `price x assets covered / asset_block` |
| `implementation_charge`, `implementation_text` | One-time implementation row |
| `scope_text` | "Scope" column text, `{assets}` replaced by the covered asset count |
| `code`, `description` | Not printed |

## 6.3 Service (`csa_service_v`)

| Field | Used for |
|---|---|
| `id`, `code`, `name`, `active`, `sequence_no` | Phase list and order |
| `assets_per_day` | PPD-PP: effort days = assets / assets per day |
| `resource_type` | `TRACKING`, `DATA_AUDIT` or `SR_EXECUTIVE` - which rate card and team size is used |
| `include_room`, `include_food` | Whether room / food cost is added for the phase |
| `default_in_scope` | Initial Yes/No of the phase on a new proposal |
| `activity` | Wording of the phase in the Word timeline table |

# 7. Frontend (Angular + Kendo)

## 7.1 Files to copy

From `frontend/src/app/` to the host (suggested folder `src/app/proposal/`):

| File | Action |
|---|---|
| `pages/proposals/proposal-list.ts` | Copy |
| `pages/proposals/proposal-editor.ts`, `.html`, `.scss` | Copy |
| `pages/masters/settings.ts` | Copy (Pricing Settings + Reference numbering) |
| `pages/dashboard/dashboard.ts` | Optional - copy if you want the proposal dashboard (uses Kendo Charts) |
| `core/models.ts` | Copy the proposal related interfaces (`Proposal*`, `Pricing*`, `DocTerms`, `Numbering`, `Totals`, `Dashboard*`); `Customer`, `Software`, `ServiceItem` only if the host has no equivalent |
| `core/api.service.ts` | Copy the proposal, config, numbering, dashboard and `lookups` methods into a new `ProposalApiService` (7.3) |
| `core/notify.service.ts`, `core/blob.ts` | Copy (or use the host's notification service; `blob.ts` saves the downloaded Word file) |
| `pages/masters/master-crud.ts`, `customers.ts`, `software.ts`, `services.ts` | **Skip** - the host has these screens |
| `app.ts`, `app.html`, `app.scss` | **Skip** (shell/menu of the standalone app) |

## 7.2 Dependencies and app setup

```bash
npm install --legacy-peer-deps \
  @progress/kendo-angular-{grid,dropdowns,inputs,buttons,dateinputs,dialog,notification,charts,intl,l10n,common,popup,layout,navigation,label,icons,indicators,toolbar,pager,treeview,utils,menu,excel-export,pdf-export}@<same version as the host's Kendo> \
  @progress/kendo-theme-default @progress/kendo-drawing @progress/kendo-data-query @progress/kendo-svg-icons @progress/kendo-licensing \
  @angular/localize @angular/animations
```

(Most of these exist already in a Kendo host - install only what is missing. If the host does not use Charts / dashboard,
`kendo-angular-charts` is not needed.)

Application settings that the screens rely on (compare with `app.config.ts` and `angular.json` of the standalone app):

| Setting | Why |
|---|---|
| `"polyfills": ["zone.js", "@angular/localize/init"]` in `angular.json` | Kendo needs `$localize` |
| `provideHttpClient()`, `provideAnimationsAsync()` | HTTP calls, Kendo animations |
| `registerLocaleData(localeEnIN)` and `{ provide: LOCALE_ID, useValue: 'en-IN' }` | Indian number grouping (4,41,562.50) through the Angular `number` / `currency` pipes. If the host uses another locale the amounts are grouped differently on screen (the Word file is not affected) |
| Kendo theme CSS in `styles` | The host already has one; the screens use `kendoButton`, grid, dropdowns, switch, dialog |
| Kendo licence | Same licence key as the host |
| Global CSS classes | Copy the helper classes from the standalone `src/styles.scss` into the host's global styles (or into the component styles): `.page-head`, `.actions`, `.card`, `.grid-2`, `.field`, `.num`, `.muted`, `.chip` (`.po`, `.saved`), `.error-text`. The `:root { --kendo-color-primary ... }` block only recolours Kendo's primary colour - skip it to keep the host theme |

## 7.3 API service

Create `ProposalApiService` from the proposal methods of `api.service.ts` (`proposals()`, `proposal(id)`, `calculate()`,
`createProposal()`, `updateProposal()`, `deleteProposal()`, `generatePo()`, `config()`, `saveConfig()`, `numbering()`,
`saveNumbering()`, `dashboard()`, `lookups.customers/software/services()`). If the host has an HTTP interceptor for the base URL / token it applies automatically;
otherwise set the base path in the `API` constant. `generatePo()` must stay `responseType: 'blob'` with
`observe: 'response'`.

## 7.4 Master lists - no change needed

`proposal-editor.ts` reads the customer, software and service lists from the module's own read-only endpoints
`GET /api/lookups/customers | software | services` (`LookupController`). They return whatever the three entities map to,
i.e. **the host data through the adapter views** (section 4.3 / 5.4). So once the views and the three `@Table` lines are
in place, the Angular proposal screen works unchanged and the host's customer / software / service APIs are not touched.

(If you prefer to call the host's own APIs, replace the three `this.api.lookups.*()` calls in `ngOnInit` by your services
and map the result to the field names of section 6: `id, name, code, active, contactName, assetCount, branchCount,
discountPercent` for customers, `id, name, active` for software, `id, code, name, assetsPerDay, defaultInScope, active`
for services.)

## 7.5 Routes and menu

```ts
{ path: 'proposals',      loadComponent: () => import('./proposal/proposal-list').then(m => m.ProposalList) },
{ path: 'proposals/new',  loadComponent: () => import('./proposal/proposal-editor').then(m => m.ProposalEditor) },
{ path: 'proposals/:id',  loadComponent: () => import('./proposal/proposal-editor').then(m => m.ProposalEditor) },
{ path: 'pricing-settings', loadComponent: () => import('./proposal/settings').then(m => m.Settings) },
```

`withComponentInputBinding()` must be enabled in `provideRouter(...)` - the editor reads `:id` through an `input()`.
Add menu entries: *Proposals* and *Pricing Settings* (admin only). Optional: a **"Create proposal"** button on the host's
customer screen linking to `/proposals/new`; to pre-select the customer add `customerId` as a query parameter and read it
in `ngOnInit` (`this.customerId = +route.snapshot.queryParams['customerId']; this.onCustomer(this.customerId)`).

# 8. Changes needed on the host's own screens

Software and Service master screens must let users maintain the pricing attributes of section 6, otherwise new software
or services get price 0 and 1 asset per day:

| Screen | Fields to add (writes to `csa_software_pricing` / `csa_service_pricing`) |
|---|---|
| Software master | Subscription price, "price covers # assets", implementation charge, implementation text, scope text |
| Service master | Assets per person per day, resource type (Tracking / Data audit / Sr executive), room expense (Y/N), food expense (Y/N), in scope by default, activity text, sequence |

Until those screens exist, maintain the two side tables with SQL (script 02 has the insert examples). The proposal screen picks up changes immediately because the lookups read the views.

# 9. Acceptance checks

1. **Startup**: application starts with `ddl-auto: validate`; no `SchemaManagementException` (view columns / types match).
2. `GET /api/numbering` returns the configured prefix; `GET /api/config` returns the rates.
3. `POST /api/proposals/calculate` with a host customer id, one software and the service lines returns totals. For the
   workbook case (5,000 assets, 3 branches, FAR / PV / DC / DM / DR in scope, tagging out) `servicesPrice` must be **441562.50**.
4. UI: *Proposals > New* - choose a customer, add software, toggle services, apply discount; the price summary updates.
5. *Save* - **Generate PO** becomes enabled only after saving; click it and open the downloaded .docx: customer name,
   address, contact, asset / branch counts, cost table, timeline and payment split are filled; no `{{` remains.
6. Edit the saved proposal, save again, generate again - the new values appear; after an issued document the reference
   becomes `...-v2`.
7. Change prefix / digits / next number in *Pricing Settings* - next proposal gets the new reference; existing ones keep theirs.
8. Roles: a user without the admin role cannot change settings; unauthenticated calls are rejected.
9. Regression: the host's own customer / software / service screens work as before (the module only reads them).

Quick API check:

```bash
curl -s localhost:8080/api/numbering
curl -s -X POST localhost:8080/api/proposals/calculate -H 'Content-Type: application/json' \
 -d '{"customerId":1,"software":[],"services":[{"serviceId":1,"inScope":true}]}'
```

# 10. Troubleshooting

## 10.1 Kendo / Angular compile errors

* `NG1010: 'imports' must be an array of components ... (not statically analyzable)` - the installed Kendo packages were
  built for a newer Angular than the host's compiler. Align versions: either upgrade Angular or install the Kendo release
  that supports it (`npm view @progress/kendo-angular-buttons peerDependencies`), all kendo-angular-* packages on **one** version,
  then delete `node_modules` and `.angular` and reinstall.
* `$localize is not defined` - add `@angular/localize/init` to the polyfills (7.2).
* `@let` not supported - Angular older than 18.1; replace the one `@let l = lineFor(...)` in `proposal-editor.html` by
  repeated `lineFor(s.serviceId)?.x` calls.

## 10.2 Backend

| Symptom | Cause / fix |
|---|---|
| `Schema-validation: wrong column type ... in table csa_customer_v` | A view column has a different type than the entity (e.g. `numeric` vs `integer`). Cast in the view (`::integer`, `::numeric(5,2)`, `::boolean`) |
| `Schema-validation: missing column` | The view lacks a column - add it (constant / null) |
| `DuplicateMappingException` | Another entity has the same entity name; the module's entities are `Csa*`, rename if the host also uses these |
| `NoSuchBeanDefinitionException` for `...Repository` | The module package is not scanned (5.3) |
| `No value for template token` warning in log | A placeholder of the Word file has no value - check the template against `Template-Placeholders.md` |
| HTTP 409 "Number range exhausted" | All numbers of the configured size are used - increase the digits in Pricing Settings |
| HTTP 409 on save with a "unique" error | The next number was set to an already used one - set a higher number |
| CORS error in the browser | Add the Angular origin to the host's CORS configuration (the module's `CorsConfig` is not copied) |
| Word file opens but layout differs | The templates were converted from `.doc` with LibreOffice - open them once in Word and re-save |

# 11. Rollback

The module is additive: remove the menu entries / routes, stop component-scanning `com.levantare.pricing`, and (only if
the data is not needed) drop the objects:

```sql
drop view  if exists csa_customer_v, csa_software_v, csa_service_v;
drop table if exists proposal_service, proposal_software, proposal, doc_sequence, pricing_config,
                     csa_service_pricing, csa_software_pricing;
```

No host table is modified by scripts 01-03.

# 12. Later upgrades

Keep the module in its own packages / folders so that a future version of the standalone project can be merged by
copying the same files again. New database changes of the standalone project are added as Flyway `V4+` files there -
ask for the equivalent `ALTER` script for the host. The standalone repository stays the source of truth for the
calculation (`PricingCalculator`) and the Word templates.
