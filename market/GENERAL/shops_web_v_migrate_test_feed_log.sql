--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-v-migrate-test-feed-log-view
create view shops_web.v_migrate_test_feed_log as
select gd.id                                                                                             gid,
       gd.name                                                                                           "name",
       fl.start_time                                                                                     generation_start_time,
       gd.release_date                                                                                   end_time,
       gd.release_date                                                                                   release_time,
       gd.mitype                                                                                         mitype,
       '-'                                                                                               sc_version,
       gd.generation_type                                                                                "type",
       fl.feed_id                                                                                        fid,
       fl.start_time                                                                                     feed_start_time,
       fl.release_time                                                                                   finish_time,
       fl.yml_time                                                                                       yml_time,
       fl.return_code                                                                                    return_code,
       fl.valid_offers                                                                                   noffers,
       case when coalesce(strpos(fl.download_status, '304'), 1) is not distinct from 0 then 0 else 1 end is_modified,
       fl.download_time                                                                                  download_time,
       fl.total_offers                                                                                   total_offers,
       fl.indexed_status                                                                                 indexed_status,
       fl.download_retcode                                                                               download_retcode,
       fl.download_status                                                                                download_status,
       fl.parse_retcode                                                                                  parse_retcode,
       null                                                                                              parse_log,
       null                                                                                              cached_parse_log,
       0                                                                                                 cpa_offers,
       fl.cpa_real_offers                                                                                cpa_real_offers,
       fl.matched_offers                                                                                 matched_offers,
       fl.discount_offers                                                                                discount_offers_count,
       null                                                                                              parse_log_parsed,
       fl.matched_cluster_offer                                                                          matched_cluster_offer_count,
       fl.feed_file_type                                                                                 feed_file_type,
       fl.market_template                                                                                market_template,
       fl.shop_id                                                                                        shop_id,
       fl.honest_discount_offers                                                                         honest_discount_offers_count,
       fl.white_promos_offers                                                                            white_promos_offers_count,
       fl.honest_white_promos_offers                                                                     honest_white_promos_offers_c,
       fst.site_name                                                                                     color,
       fl.cpc_real_offers                                                                                cpc_real_offers
from shops_web.feed_log_history fl
         join shops_web.generation_meta gd
              on fl.meta_id = gd.id
         join shops_web.feed_site_type fst
              on fst.id = gd.site_type
where gd.indexer_type = 1

