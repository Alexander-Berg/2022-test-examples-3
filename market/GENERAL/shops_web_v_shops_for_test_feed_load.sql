--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-v-shops-for-test-feed-load-view
create view shops_web.v_shops_for_test_feed_load as
select datasource_id, updated_at
from shops_web.datasources_in_testing
where in_progress = 1
  and fatal_cancelled = 0
  and datasource_id in
      (
          select entity_id
          from shops_web.param_value
          where param_type_id = 29
            and num_value = 1
      )

