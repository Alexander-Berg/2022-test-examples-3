/*
 Магазины, которые отключились более 30 дней назад, за исключением
   1) магазинов, находящих в тестовом индексе на полной проверке,
   2) магазинов, которые прошли тестирование за последние 30 дней
*/
create or replace view shops_web.v_datasources_need_testing
as
select v1.datasource_id from
(
    select min(from_time) min_from, datasource_id from v_current_active_period vcap group by datasource_id
) v1
where v1.min_from < sysdate - 30
minus
select datasource_id from datasources_in_testing where testing_type = 0
minus
select datasource_id from ds_active_period where period_type_code=6
and to_time is not null and to_time between sysdate-30 and sysdate
/