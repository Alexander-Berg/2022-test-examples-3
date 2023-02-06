--liquibase formatted sql
--changeset sergeyvalkov:MBI-67401-create-v_shops_in_testing
create view shops_web.v_shops_in_testing as
select d.id,
       d.dit_id,
       d.name,
       d.ready,
       d.approved,
       d.in_progress,
       d.cancelled,
       d.fatal_cancelled,
       d.test_loading,
       d.test_quality,
       d.test_cloning,
       case when (dap.from_time - d.created_at < interval '1' hour) then 1 else 0 end new_shop,
       d.shop_program
from (
         select ds.id,
                ds.created_at,
                ds.name,
                dit.id as                                                     dit_id,
                dit.ready,
                dit.approved,
                dit.in_progress,
                dit.cancelled,
                dit.fatal_cancelled,
                dit.shop_program,
                sum(case pnv.param_type_id when 29 then pnv.value else 0 end) test_loading,
                sum(case pnv.param_type_id when 30 then pnv.value else 0 end) test_quality,
                sum(case pnv.param_type_id when 31 then pnv.value else 0 end) test_cloning
         from shops_web.datasources_in_testing dit
                  join shops_web.datasource ds on ds.id = dit.datasource_id
                  left join shops_web.v_param_number_value pnv
                            on pnv.entity_id = ds.id and pnv.param_type_id in (29, 30, 31)
         group by ds.id,
                  ds.created_at,
                  ds.name,
                  dit.id,
                  dit.ready,
                  dit.approved,
                  dit.in_progress,
                  dit.cancelled,
                  dit.fatal_cancelled,
                  dit.shop_program
     ) d
         left join shops_web.ds_active_period dap
                   on (dap.datasource_id = d.id and dap.period_type_code = 6 and dap.from_time < current_timestamp and
                       dap.to_time is null);
