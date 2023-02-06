--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-test-generation-view
create view shops_web.test_generation as
select "id",
       "name",
       "start_time",
       "end_time",
       "release_time",
       "mitype",
       "sc_version",
       "type"
from shops_web.test_generation_and_delta
where type = 0

