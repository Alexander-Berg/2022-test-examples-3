--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-v-datasources-need-testing-view
create view shops_web.v_datasources_need_testing as
select v1.datasource_id
from (
         select min(from_time) min_from, datasource_id
         from shops_web.v_current_active_period vcap
         group by datasource_id
     ) v1
where v1.min_from < current_timestamp - interval '30 day'
    except
select datasource_id
from shops_web.datasources_in_testing
where testing_type = 0
    except
select datasource_id
from shops_web.ds_active_period
where period_type_code = 6
  and to_time is not null
  and to_time between current_timestamp - interval '30 day' and current_timestamp

