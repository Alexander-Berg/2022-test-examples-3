--liquibase formatted sql
--changeset sergeyvalkov:MBI-79617
create or replace view shops_web.v_last_test_generation
        (
         id,
         name,
         start_time,
         end_time,
         release_time,
         mitype,
         sc_version,
         type
            )
as
select id,
       name,
       start_time,
       end_time,
       release_time,
       mitype,
       sc_version,
       type
from shops_web.test_generation_and_delta
where type = 0
order by release_time desc, id desc
    fetch next 1 rows only;
