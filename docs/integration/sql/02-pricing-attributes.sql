-- =====================================================================================================
-- Pricing attributes the proposal module needs on the host's SOFTWARE and SERVICE masters.
-- Option B (recommended, nothing in the host tables is changed): two side tables keyed by the host master id.
-- Option A (if you prefer): add the same columns to the host tables and read them directly in the views.
-- <host_software_table> / <host_service_table> and "id" below are placeholders - see the example in 03.
-- =====================================================================================================

create table if not exists csa_software_pricing (
    software_id           bigint        primary key,        -- host software id
    scope_text            varchar(2000),                    -- text in the "Scope" column; {assets} = covered assets
    price                 numeric(14,2) not null,           -- subscription price for one block of asset_block assets
    asset_block           integer       not null default 1, -- e.g. 3000: price covers 3,000 assets, more scales proportionately
    implementation_charge numeric(14,2) not null default 0, -- one-time charge
    implementation_text   varchar(2000)                     -- text of the implementation row
);

create table if not exists csa_service_pricing (
    service_id       bigint        primary key,             -- host service id
    activity         varchar(500),                          -- wording in the timeline table
    sequence_no      integer       not null default 0,      -- order of the phases
    assets_per_day   numeric(12,2) not null,                -- PPD-PP (assets per person per day)
    resource_type    varchar(20)   not null,                -- TRACKING | DATA_AUDIT | SR_EXECUTIVE
    include_room     boolean       not null default false,
    include_food     boolean       not null default false,
    default_in_scope boolean       not null default true
);

-- Example data for the sample workbook phases. Replace the ids with the ids of the matching HOST service rows.
-- insert into csa_service_pricing (service_id, activity, sequence_no, assets_per_day, resource_type, include_room, include_food, default_in_scope) values
--  (<id of FAR>, 'Fixed Asset (FAR) book Data Analysis & Cleaning (line Items)',        1,  800, 'DATA_AUDIT', false, false, true),
--  (<id of PV >, 'Fixed Assets Physical Tracking and Verification with Tagging',        2,  150, 'TRACKING',   true,  true,  true),
--  (<id of DC >, 'Data Audit - Invoice No, PO No and Quantity check',                   3,  500, 'DATA_AUDIT', false, false, true),
--  (<id of DM >, 'Data Matching - Physical verified Assets vs FAR',                     4,  750, 'DATA_AUDIT', false, false, true),
--  (<id of DR >, 'Variance Report Submission',                                          5,  100, 'TRACKING',   false, false, true),
--  (<id of TG >, 'QR Code Tagging of verified assets',                                  6,   70, 'TRACKING',   true,  true,  false);
