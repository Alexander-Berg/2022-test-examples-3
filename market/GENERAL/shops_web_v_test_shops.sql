--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-v-test-shops-view
create view shops_web.v_test_shops as
select 774 datasource_id
union all
select 707
union all
select 101663
union all
select 102665
union all
select 64407
union all
select 81832
union all
select 41870
union all
select 48398
union all
select 48401
union all
select 70608
union all
select 70609
union all
select 70610
union all
select 70611
union all
select 70612
union all
select 70613
union all
select entity_id
from shops_web.param_value
where param_value_id = 32
  and num_value = 1

