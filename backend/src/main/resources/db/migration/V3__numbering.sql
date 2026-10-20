-- Reference number format, kept in the database and editable from the Pricing Settings screen
alter table pricing_config add column proposal_prefix varchar(40) not null default 'LTPL/PRO';
alter table pricing_config add column po_prefix       varchar(40) not null default 'LTPL/PO';
alter table pricing_config add column include_financial_year boolean not null default true;
alter table pricing_config add column reset_each_year boolean not null default true;
alter table pricing_config add column number_width integer not null default 5;
alter table pricing_config add column series_start_no integer not null default 1;
