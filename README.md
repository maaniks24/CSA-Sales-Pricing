# CSA Sales Pricing

Small web application that replaces the pricing workbook and the hand-edited Word proposals.

1. Pick a **customer** (customer master gives contact, asset count, branch count and the customer's discount %).
2. Add **software** and **services** (software master / service master).
3. Apply the **discount**, the price is **calculated live** (same formulas as the sample workbook).
4. **Save** - the *Generate PO* button is enabled only for a saved proposal without pending edits.
5. **Generate PO** downloads a **Word document built from the original templates** (logo, header/footer, all
   standard text) with customer, amounts, scope and terms filled in from the application.

| Layer | Tech |
|---|---|
| `frontend/` | Angular 20 + Kendo UI for Angular 25 (Grid, DropDowns, Inputs, Charts, Dialog, AppBar ...) |
| `backend/`  | Spring Boot 3.5 (Java 21), Spring Data JPA, Flyway, Apache POI (docx) |
| Database    | PostgreSQL 16 (H2 in PostgreSQL mode is used by the tests) |

## Run it

```bash
# 1. database
docker compose up -d db          # or any PostgreSQL 16: db csa_pricing, user/password csa/csa

# 2. backend  (http://localhost:8080, Flyway creates the schema and seeds the sample data)
cd backend && mvn spring-boot:run

# 3. frontend (http://localhost:4200, /api is proxied to :8080)
cd frontend && npm install --legacy-peer-deps && npm start
```

Everything in one go: `docker compose up --build` (UI on http://localhost:4200). Backend settings are environment
variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `REF_PREFIX` (default `LTPL`), `CORS_ALLOWED_ORIGINS`.

Tests: `cd backend && mvn test` (pricing formulas against the workbook figures, whole API flow incl. generating
both documents).

> **Kendo licence.** Kendo UI for Angular needs a licence key (free trial or purchased). Without it everything works
> but a banner and a watermark are shown. Activate with `npx kendo-ui-license activate` (key file
> `kendo-ui-license.txt`, not committed) - see https://www.telerik.com/kendo-angular-ui/components/my-license/.

## Screens

* **Dashboard** - KPIs, proposal value of the last 6 months, status split, top customers, recent proposals.
* **Proposals** - list, open, delete, regenerate the document.
* **Proposal editor** - the main process (customer -> software -> services -> parameters, live price summary).
* **Customers / Software / Services** - master data (Kendo grid + dialog).
* **Pricing Settings** - rate card, branch / travel cost, overhead %, profit %, GST, validity, team sizes, payment split.
  They are copied into each proposal when it is created, so later changes do not alter saved proposals.

## Pricing logic (`PricingCalculator`)

Taken from the sheet *Template for Apps*:

```
effort days (phase) = assets / PPD-PP                  only when scope = Yes
room / food         = effort days * rate               only for phases flagged room / food
salary              = effort days * salary rate        rate card chosen by the phase's resource type
sub-total           = phases + branches * cost per branch + travel (+ senior executive days * rate)
overheads           = sub-total * overhead %
profit              = (sub-total + overheads) * profit %
services price      = sub-total + overheads + profit                 (workbook: 441,562.50 -> 88.3125 per asset)
software            = price * assets covered / price block + one-time implementation   (90,000 per 3,000 assets)
discount            = (services + software) * discount %      (default from the customer, editable)
GST                 = (total - discount) * GST %
```

Phase set-up in the service master reproduces the workbook (`FAR`, `DC`, `DM` data-audit salary only; `PV`, `TG`
tracking with room + food; `DR` tracking salary only). The workbook uses the data-audit salary cell for `PV`; the
app uses the tracking salary - identical today because both are 1,000.
"Working days" in the timeline = ceil(effort days / team size).

## Document generation

Templates live in `backend/src/main/resources/templates/` and were produced from the two sample documents by
`tools/build_templates.py` (customer specific text replaced by `{{tokens}}`; samples are in `docs/samples/`).

| Proposal contains | Template |
|---|---|
| services (with or without software) | `services-proposal.docx` (Apexon sample); software lines are added to its cost table |
| software only | `software-proposal.docx` (UD Trucks sample) |

Token rules (`DocxTemplateEngine`): `{{name}}` simple value, `{{list.field}}` repeats the table row per element
(cost table `costRows`, timeline `phases`), `{{?flag}}` drops the row when the flag is false. To change wording edit the
.docx in Word (keep the tokens) - no code change needed; to add a new value add it in `ProposalDocumentBuilder`.

Filled automatically: reference no (`LTPL/PRO/<FY>/<seq>-v<version>`), date, customer name / address, contact person,
asset and branch counts, cost table (services, software, implementation, discount, total, GST, grand total),
timeline (phases in scope, working days), milestone payment %, team sizes, validity and start days.
Saving a proposal that already had its PO generated creates the next version (`-v2`). PO no
(`LTPL/PO/<FY>/<seq>`) and PO date are assigned the first time the document is generated.

## API (`/api`)

`GET/POST/PUT/DELETE customers | software | services`, `GET/PUT config`, `GET dashboard`,
`GET/POST proposals`, `GET/PUT/DELETE proposals/{id}`, `POST proposals/calculate` (preview, nothing saved),
`POST proposals/{id}/generate-po` (returns the .docx).

## Assumptions to confirm

* The sample documents are proposals; the "PO" is generated from them, so the output keeps the proposal layout and the
  PO no / PO date are stored on the record (the sheet has the fields PO No / PO Date / Status).
* The meaning of the phase codes is inferred: FAR register study & cleaning, PV physical verification, DC data check,
  DM data matching, DR reporting, TG tagging - names are editable in the Services master.
* Software price scales proportionately with assets ("cost will increase proportionately"). Discount applies to
  software and services together.
* No authentication yet (internal tool); put it behind your SSO / reverse proxy or add Spring Security.
* Generated files come from a LibreOffice conversion of the old `.doc` samples; open them once in Word to check
  pagination of your final wording.
