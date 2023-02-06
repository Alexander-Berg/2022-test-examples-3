--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-monitor-market-test-indexer-view
create view shops_web.monitor_market_test_indexer as
select case
           when cnt > (select count(-1) / 3
                       from shops_web.feed_log_history
                       where meta_id = (select id
                                        from shops_web.v_last_full_generations
                                        where indexer_type = 1
                                          and site_type = 0)) then 2
           else 0
           end as result,
       case
           when cnt > 0
               then cnt
                        || ' feeds has been broken in the last ('
                        || "name"
                        || ') market premoderation indexation, first feed id = '
               || fid
           else ''
           end as description
from (
         select min(feed_id) fid, max("name") "name", count(-1) cnt
         from shops_web.v_last_full_generations meta
                  left join shops_web.feed_log_history flh on meta.id = flh.meta_id
         where site_type = 0
           and flh.indexer_type = 1
           and return_code > 2) as fnc

--changeset a-kazachenko:MBI-83172
insert into monitor.monitoring (host,
                                service,
                                period_min,
                                monitor_view_name,
                                monitor_description)
values ('market_mbi_shops', 'mbi-market-10', 5, 'shops_web.monitor_market_test_indexer', '');
