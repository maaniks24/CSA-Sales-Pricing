-- =====================================================================================================
-- Adapter views: present the HOST application's customer / software / service tables to the proposal module
-- in the shape its entities expect (CsaCustomer, CsaSoftware, CsaServiceItem).
--
-- THE HOST TABLE AND COLUMN NAMES BELOW ARE AN EXAMPLE (crm_customer, crm_software, crm_service) - EDIT THE
-- RIGHT-HAND SIDE OF EVERY "as" TO YOUR COLUMNS. Every column must exist in the view (use a constant / null
-- when the host has no equivalent) and keep the declared data types - Hibernate validates them.
-- After creating the views point the three entities at them (@Table(name = "csa_customer_v") etc.).
-- =====================================================================================================

create or replace view csa_customer_v as
select c.id::bigint                              as id,
       c.code::varchar                           as code,
       c.name::varchar                           as name,
       concat_ws(E'\n', c.addr1, c.addr2)::varchar as address,        -- printed address block, one line per \n
       c.city::varchar                           as city,
       c.industry::varchar                       as vertical,
       c.contact::varchar                        as contact_name,     -- addressee: "Hi <contact_name>"
       c.email::varchar                          as contact_email,
       c.phone::varchar                          as contact_phone,
       coalesce(c.assets, 0)::integer            as asset_count,
       greatest(coalesce(c.branches, 1), 1)::integer as branch_count,
       coalesce(c.disc, 0)::numeric(5,2)         as discount_percent, -- customer based discount
       (c.status = 'A')::boolean                 as active
from crm_customer c;

create or replace view csa_software_v as
select s.id::bigint                              as id,
       s.code::varchar                           as code,
       s.name::varchar                           as name,
       s.descr::varchar                          as description,
       p.scope_text::varchar                     as scope_text,
       coalesce(p.price, 0)::numeric(14,2)       as price,
       coalesce(p.asset_block, 1)::integer       as asset_block,
       coalesce(p.implementation_charge, 0)::numeric(14,2) as implementation_charge,
       p.implementation_text::varchar            as implementation_text,
       (s.status = 'A')::boolean                 as active
from crm_software s
left join csa_software_pricing p on p.software_id = s.id;

create or replace view csa_service_v as
select s.id::bigint                              as id,
       s.code::varchar                           as code,
       s.name::varchar                           as name,
       p.activity::varchar                       as activity,
       coalesce(p.sequence_no, 0)::integer       as sequence_no,
       coalesce(p.assets_per_day, 1)::numeric(12,2) as assets_per_day,
       coalesce(p.resource_type, 'TRACKING')::varchar as resource_type,
       coalesce(p.include_room, false)::boolean  as include_room,
       coalesce(p.include_food, false)::boolean  as include_food,
       coalesce(p.default_in_scope, false)::boolean as default_in_scope,
       (s.status = 'A')::boolean                 as active
from crm_service s
left join csa_service_pricing p on p.service_id = s.id;
