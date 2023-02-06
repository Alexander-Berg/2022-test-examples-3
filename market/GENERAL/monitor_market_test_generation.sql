--liquibase formatted sql
/*
идут тестовые генерации на planeshift3.
Если поколений нет уже больше 4 часов, то CRIT.
Если в поколениях за последние 2 часа есть битые, то CRIT.
*/

--changeset batalin:MBI-56515
create or replace view shops_web.monitor_market_test_generation (
   result,
   description
)
as
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
          select
                 count(id)                                                                       generations_4h,
                 sum(case
                         when sysdate - cast(release_time as date) <= 2 / 24 and feeds = 0 then 1
                         else 0 end)                                                             bad_generations_2h
          from (
                   select meta.id,
                          min(meta.release_date) release_time,
                          count(flh.feed_id)     feeds
                   from shops_web.generation_meta meta
                            left join shops_web.feed_log_history flh
                                      on (meta.id = flh.meta_id and flh.return_code < 3)
                   where meta.release_date > sysdate - 4 / 24
                     and meta.indexer_type = 1
                     and meta.generation_type = 0
                     and meta.site_type = 0
                     and flh.IS_IN_INDEX = 1
                   group by meta.id
               )
      )
;

