--liquibase formatted sql

--changeset yakun:MBI-32508_add_cpc_table_for_testing
CREATE OR REPLACE VIEW shops_web.v_shop_cpc_need_info_test (
  datasource_id,
  noline,
  home_region,
  local_dlv_region,
  dlv_regions,
  phone,
  datasource_domain,
  org_info,
  ogrn_is_bad,
  ship_info,
  outlet,
  contact,
  datasource_name
) AS
    select
    datasource_id,
    noline,
    home_region,
    local_dlv_region,
    dlv_regions,
    phone,
    datasource_domain,
    org_info,
    ogrn_is_bad,
    ship_info,
    outlet,
    contact,
    datasource_name
from shops_web.v_shops_need_info_test
  where noline = 1
    or home_region = 1
    or contact = 1
    or local_dlv_region = 1
    or org_info = 1
    or ship_info = 1
    or phone = 1
    or outlet = 1
    or outlet_to_publish = 1
    or datasource_domain = 1
    or datasource_name = 1;

--changeset yakun:MBI-32765_drop_test_views_v_shop_cpc_need_info_test
drop view shops_web.v_shop_cpc_need_info_test;