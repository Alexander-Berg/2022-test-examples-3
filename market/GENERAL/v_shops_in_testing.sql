--liquibase formatted sql

--changeset vbauer:MBI-21853-drop-synonym-V_SHOPS_IN_TESTING_ADMIN
call mbi_core.ddl_helper.drop_synonym_if_exists('MBI_ADMIN','V_SHOPS_IN_TESTING_ADMIN');

--changeset vbauer:MBI-21853-V_SHOPS_IN_TESTING.4
create or replace view shops_web.v_shops_in_testing as
select
  d.id, d.dit_id, d.name, d.ready, d.approved, d.in_progress, d.cancelled,
  d.fatal_cancelled, d.test_loading, d.test_quality, d.test_cloning,
  case when dap.from_time-d.created_at < 1/24 then 1 else 0 end new_shop,
  d.shop_program
from (
  select
    ds.id, ds.created_at, ds.name, dit.id dit_id,
    dit.ready, dit.approved, dit.in_progress, dit.cancelled, dit.fatal_cancelled,
    dit.shop_program,
    sum(case when pnv.param_type_id=29 then pnv.value else 0 end) test_loading,
    sum(case when pnv.param_type_id=30 then pnv.value else 0 end) test_quality,
    sum(case when pnv.param_type_id=31 then pnv.value else 0 end) test_cloning
from
  shops_web.datasources_in_testing dit, shops_web.datasource ds, shops_web.v_param_number_value pnv
where
  dit.datasource_id=ds.id
  and pnv.param_type_id in (29, 30, 31)
  and pnv.entity_id(+)=ds.id
  group by ds.id, ds.created_at, ds.name,
  dit.id, dit.ready, dit.approved, dit.in_progress, dit.cancelled, dit.fatal_cancelled, dit.shop_program
) d
left join ds_active_period dap on (dap.datasource_id=d.id and dap.period_type_code=6 and dap.from_time<sysdate and dap.to_time is null);
