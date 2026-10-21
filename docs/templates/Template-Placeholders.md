# Proposal / PO document - values filled from the application

Review files (open in Word, the dynamic parts show as `{{placeholder}}`):

* `Software-Proposal-Template.docx` - used when the proposal has software only (based on the UD Trucks sample)
* `Services-Proposal-Template.docx` - used when the proposal has services, with or without software (based on the Apexon sample)

Everything that is NOT a `{{placeholder}}` is fixed standard text of the sample documents.
Money is printed Indian style (7,88,000 / 4,41,562.50, no decimals when the amount is whole), dates as dd/mm/yyyy.

The shipped copies used by the application are in `backend/src/main/resources/csa-templates/`
(copy a reviewed/edited .docx over them, keep the placeholders).

## 1. Simple placeholders

| Placeholder | Appears in | Value comes from | Example |
|---|---|---|---|
| `{{refNo}}` | Both - top left "Ref No:" | Generated on first save: `LTPL/PRO/<financial year>/<5-digit running no>-v<version>`. Version goes up when a proposal is edited after its document was issued | LTPL/PRO/2026-27/00044-v1 |
| `{{proposalDate}}` | Both - top right | Proposal date field of the editor (default today) | 30/09/2026 |
| `{{customerName}}` | Both - address block, "Hi" paragraph text, scope, training / customization / reconciliation sentences | Customer master - Customer name | UD Trucks India Pvt. Ltd. |
| `{{customerAddress}}` | Both - address block (one line per line entered) | Customer master - Address | 2nd Floor, Lake View Building,... |
| `{{contactName}}` | Both - "Hi ...". Services template also: "report the gaps for ... to provide the details" | Proposal - Contact person (defaults from Customer master - Contact person; "Sir / Madam" if empty) | Vijay |
| `{{assetCount}}` | Software: "Coverage for about ... assets". Services: "Approx ... assets" and Audit Locations table (Asset Count) | Proposal - No. of assets (defaults from Customer master) | 7,000 |
| `{{branchCount}}` | Services: Audit Locations table (Location) and "across ... locations" | Proposal - No. of branches (defaults from Customer master) | 5 |
| `{{gstPercent}}` | Both - "GST as applicable ... (18%)", Software also "Taxes: GST 18%" | Proposal - Document terms - GST % (default from Pricing Settings) | 18 |
| `{{validityDays}}` | Both - "Validity: valid for a period of ... days" | Proposal - Document terms - Validity (days) | 10 |
| `{{advancePercentSoftware}}` | Software - Payment 1 "... % advance along with purchase order" | Fixed 100 (software is paid 100% in advance) | 100 |
| `{{commencementDays}}` | Services - "Project Commencement - initiate within ... days" | Proposal - Document terms - Project start (days after PO) | 10 |
| `{{pay1}}` / `{{pay2}}` / `{{pay3}}` | Services - milestone table payment column: advance with PO / on completion of physical verification / after completion report | Proposal - Document terms - Payment % (must total 100) | 50 / 40 / 10 |
| `{{trackingTeamSize}}` | Services - Team size "Asset Audit Team (Tracking Team and Barcoding) - ..." | Proposal - Document terms - Tracking team size | 4 |
| `{{dataTeamSize}}` | Services - Team size "Data Audit Team - ..." | Proposal - Document terms - Data audit team size | 1 |

## 2. Cost table rows - `{{costRows.label}}`, `{{costRows.details}}`, `{{costRows.scope}}`, `{{costRows.amount}}`

The row holding these placeholders is repeated; the application writes these rows in this order
(label column exists only in the services template):

| Row | Label | Details | Scope | Amount |
|---|---|---|---|---|
| Services (only if at least one service is in scope) | Services | "Enterprise wide" + one "- service name" line per service in scope (Service master names) | "Cost of Physical Tracking and Audit for up to <assets> assets" + "cost will increase proportionately" note | Services price = (phases + branches x cost per branch + travel) + overheads + profit |
| Software, one per software item | Software | Software master - Name | Software master - Scope text, `{assets}` replaced by the assets covered | Software price x assets covered / price block |
| Implementation, one per software with a charge | Software | Software master - Implementation text | (blank) | Software master - Implementation charge |
| Discount (only if discount > 0) | | "Less: Discount @ x%" | | (discount amount) = (services + software) x discount % |
| Total | | Total (excluding GST) | | gross - discount |
| GST | | GST @ 18% | | total x GST % |
| Grand total | | Grand Total (including GST) | | total + GST |

## 3. Timeline table (services template) - `{{phases.no}}`, `{{phases.activity}}`, `{{phases.assets}}`, `{{phases.days}}`

One row per service phase that is "in scope" in the proposal, in Service master order:

| Placeholder | Value |
|---|---|
| `{{phases.no}}` | 1, 2, 3 ... |
| `{{phases.activity}}` | Service master - Activity text (falls back to the service name) |
| `{{phases.assets}}` | Proposal - No. of assets |
| `{{phases.days}}` | Working days = effort days / team size, rounded up. Effort days = assets / assets-per-person-per-day (Excel "Effort in Days"); team size = tracking team for Tracking services, data audit team for Data audit services |

`{{nextPhase}}` = number after the last phase row, used for the fixed row "On sign of ... submit the Assets list with audit report".
`{{?hasSoftware}}` in front of the "Smart Assets Software Migration" row is a switch: the row is printed only when the proposal contains software.

## 4. Fixed text (not from the application)

Header / footer and logo, introduction and approach paragraphs, scope of work, out-of-scope asset classes table,
workflow stages, deliverables, benefits, milestone deliverables text, terms other than the values above,
resources required from client, security / VA-PT section, signature block ("Mir Fazlee Ali").
To change any of it, edit the .docx in Word and keep the `{{...}}` placeholders intact.

## 5. Not used in the document

Notes (internal), room / food / salary amounts, overheads and profit amounts, per-asset rate - these stay inside the application.
