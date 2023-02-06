create or replace view shops_web.v_test_shops (datasource_id)
as select 774 datasource_id from dual
union all
select 707 from dual
union all
select 101663 from dual
union all
select 102665 from dual
union all
select 64407 from dual
union all
select 81832 from dual
union all
select 41870 from dual
union all
select 48398 from dual
union all
select 48401 from dual
union all
select 70608 from dual
union all
select 70609 from dual
union all
select 70610 from dual
union all
select 70611 from dual
union all
select 70612 from dual
union all
select 70613 from dual
union all select entity_id from shops_web.param_value where param_value_id = 32 and num_value = 1
/