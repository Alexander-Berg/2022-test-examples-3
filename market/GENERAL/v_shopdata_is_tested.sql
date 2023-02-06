--liquibase formatted sql

--changeset batalin:MBI-52305.2

/*
Находится ли магазин на проверке.
Определяет, нужно ли раскатывать магазин в planeshift/market-preview.

Значение считается на основе следующих параметров:
- datafeed#is_enabled - это флаг того, что указанный источник данных используется при работе (должно быть 1)
- param_value#10 - это тестовость магазина (должно быть 1)
- param_value#76 - Статус доступности скидок (должно быть 'NEW')
- feature_type#90 - это статус участия магазина в программе "Акции" по CPC (должно быть 'NEW')
 */

create or replace view shops_web.v_shopdata_is_tested (
         datasource_id,
         datafeed_id,
         is_tested
    ) as
select
       df.datasource_id,
       df.id,
       case
           when (nvl(shop_testing_param.num_value, 0) = 1 or
                 discounts_status_param.str_value = 'NEW' or
                 promo_cpc.status = 'NEW') and
                df.is_enabled = 1
           then 1
         else 0
       end
from
     shops_web.v_shopdata_datafeed df
        left join (select entity_id, num_value from shops_web.param_value where param_type_id = 10) shop_testing_param
            on df.datasource_id = shop_testing_param.entity_id

        left join (select entity_id, str_value from shops_web.param_value where param_type_id = 76) discounts_status_param
            on df.datasource_id = discounts_status_param.entity_id

        left join (select datasource_id, status from shops_web.feature where feature_type = 90) promo_cpc
            on df.datasource_id = promo_cpc.datasource_id
;
