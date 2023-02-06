--liquibase formatted sql
--changeset dgir:MBI-80305-v-shopdata-is-tested
create or replace view shops_web.v_shopdata_is_tested (datasource_id, datafeed_id, is_tested) as
select df.datasource_id,
       df.id,
       case
           when (coalesce(shop_testing_param.num_value, 0) = 1 or
                 discounts_status_param.str_value = 'NEW' or
                 promo_cpc.status = 'NEW') and
                df.is_enabled = 1
               then 1
           else 0
           end
from shops_web.v_shopdata_datafeed df
         left join (select entity_id, num_value from shops_web.param_value where param_type_id = 10) shop_testing_param
                   on df.datasource_id = shop_testing_param.entity_id

         left join (select entity_id, str_value
                    from shops_web.param_value
                    where param_type_id = 76) discounts_status_param
                   on df.datasource_id = discounts_status_param.entity_id

         left join (select datasource_id, status from shops_web.feature where feature_type = 90) promo_cpc
                   on df.datasource_id = promo_cpc.datasource_id;
