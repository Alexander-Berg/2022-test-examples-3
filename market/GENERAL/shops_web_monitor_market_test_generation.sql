--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-monitor-market-test-generation-view
create view shops_web.monitor_market_test_generation as
select case
           when generations_4h = 0 then monitor.critical_during_the_day(7, 21)
           when bad_generations_2h > 0 then monitor.critical_during_the_day(7, 21)
           else 0
           end as result,
       case
           when generations_4h = 0
               then 'no market generations on the testing base (planeshift3) within last 4 hours'
           when bad_generations_2h > 0
               then 'bad market generations on the testing base (planeshift3) within last 2 hours'
           else null
           end as description
from (
         select count(id)           generations_4h,
                sum(case
                        when current_timestamp - cast(release_time as date) <= interval '2 hour' and feeds = 0 then 1
                        else 0 end) bad_generations_2h
         from (
                  select meta.id,
                         min(meta.release_date) release_time,
                         count(flh.feed_id)     feeds
                  from shops_web.generation_meta meta
                           left join shops_web.feed_log_history flh
                                     on (meta.id = flh.meta_id and flh.return_code < 3)
                  where meta.release_date > current_timestamp - interval '4 hour'
                    and meta.indexer_type = 1
                    and meta.generation_type = 0
                    and meta.site_type = 0
                    and flh.is_in_index = 1
                  group by meta.id
              ) as irtf
     ) as g4hbg2h

--changeset a-kazachenko:MBI-83172
insert into monitor.monitoring (host,
                                service,
                                period_min,
                                monitor_view_name,
                                monitor_description)
values ('market_mbi_shops', 'mbi-market-14', 5, 'shops_web.monitor_market_test_generation',
        'идут тестовые генерации на planeshift2');
