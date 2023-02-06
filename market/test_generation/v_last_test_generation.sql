--liquibase formatted sql

--changeset vbauer:mbi-28096-v_last_test_generation
create or replace view shops_web.v_last_test_generation
as
 select * from (
   select *
   from shops_web.test_generation_and_delta
   where type = 0
   order by release_time desc, id desc
 ) where rownum = 1;
