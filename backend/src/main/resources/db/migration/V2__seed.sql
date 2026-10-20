-- Pricing parameters taken from the sample workbook (Template for Apps)
insert into pricing_config (id, tracking_room, tracking_food, tracking_salary, data_audit_salary,
    sr_exec_room, sr_exec_food, sr_exec_salary, cost_per_branch, travel_cost, overhead_percent, profit_percent,
    gst_percent, validity_days, commencement_days, tracking_team_size, data_team_size, pay1, pay2, pay3)
values (1, 1000, 500, 1000, 1000, 2000, 1000, 2500, 5000, 25000, 50, 50, 18, 10, 10, 4, 1, 50, 40, 10);

-- Service phases (PPD-PP = assets handled per person per day)
insert into service_master (code, name, activity, sequence_no, assets_per_day, resource_type, include_room, include_food, default_in_scope) values
 ('FAR', 'Fixed Asset Register (FAR) Study & Cleaning', 'Fixed Asset (FAR) book Data Analysis & Cleaning (line Items)', 1,  800, 'DATA_AUDIT', false, false, true),
 ('PV',  'Physical Verification & Tracking',            'Fixed Assets Physical Tracking and Verification with Tagging', 2,  150, 'TRACKING',   true,  true,  true),
 ('DC',  'Data Check (Invoice / PO / Quantity)',       'Data Audit - Invoice No, PO No and Quantity check',           3,  500, 'DATA_AUDIT', false, false, true),
 ('DM',  'Data Matching',                              'Data Matching - Physical verified Assets vs FAR',             4,  750, 'DATA_AUDIT', false, false, true),
 ('DR',  'Reporting & Variance Report',                'Variance Report Submission',                                  5,  100, 'TRACKING',   false, false, true),
 ('TG',  'QR Code Tagging',                            'QR Code Tagging of verified assets',                          6,   70, 'TRACKING',   true,  true,  false);

insert into software (code, name, description, scope_text, price, asset_block, implementation_charge, implementation_text) values
 ('SAP-CLOUD', 'Smart Assets Pro Software - Cloud Version',
  'Cloud (SaaS) fixed assets management software with unlimited users and locations',
  'SaaS with 1-year subscription cost' || chr(10) || 'For about {assets} assets' || chr(10) || '' || chr(10) || 'Please note that if the number of assets exceeds {assets} the cost will increase proportionately',
  90000, 3000, 25000,
  'Initial one-time charge for Implementation' || chr(10) || 'Data Migration for assets - Data migration from Excel to Smart Assets Cloud' || chr(10) || 'Depreciation calculation for the current financial year' || chr(10) || 'Training');

insert into customer (code, name, address, city, vertical, contact_name, contact_email, asset_count, branch_count, discount_percent) values
 ('C001', 'UD Trucks India Pvt. Ltd.', '2nd Floor, Lake View Building,' || chr(10) || 'Bagmane Tech Park Rd,' || chr(10) || 'C V Raman Nagar,' || chr(10) || 'Bengaluru 56009', 'Bengaluru', 'Automobile', 'Vijay', null, 7000, 1, 0),
 ('C002', 'Apexon', '7th Floor, Olympia Teknos' || chr(10) || 'Plot No: 28, South Phase' || chr(10) || 'SIDCO Industrial Estate' || chr(10) || 'Guindy,' || chr(10) || 'Chennai - 600032', 'Chennai', 'IT Services', 'Prasanna Srinivasa', null, 10000, 5, 5);
