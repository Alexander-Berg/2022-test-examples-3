--liquibase formatted sql
--changeset stani:test-ora
create view shops_web.supplier_delivery_mode
            (
             id,
             fulfillment,
             crossdock,
             dropship,
             click_and_collect,
             dropship_by_seller
                )
as
with crossdock as (
    select datasource_id as id
    from shops_web.feature
    where feature_type = 1002 -- CROSSDOCK, Хранение товаров на складе партнера, доставка силами Беру.
      and status is not null
      and status != 'REVOKE'
      and not exists(select 1
                     from shops_web.feature_open_cutoff crossdock_hidden
                     where crossdock_hidden.datasource_id = feature.datasource_id
                       and crossdock_hidden.feature_type = feature.feature_type
                       and crossdock_hidden.cutoff_type = 1010)
),
     dropship as (
         select entity_id as id
         from shops_web.param_value
         where param_type_id = 115 -- DROPSHIP_AVAILABLE, Признак участия поставщика в программе работы по прямым поставкам
           and num_value = 1
     ),
     click_and_collect as (
         select param_value.entity_id as id
         from shops_web.param_value
                  inner join dropship
                             on param_value.entity_id = dropship.id
         where param_type_id = 144 -- CLICK_AND_COLLECT, Признак, работает ли поставщик по модели Click and collect
           and param_value.num_value = 1
     ),
     fulfillment as (
         select id
         from shops_web.supplier
                  except
             select id
         from crossdock
             except
         select id
         from dropship
             except
         select id
         from click_and_collect
     ),
-- DSBS - дропшип поставщики, доставляющие самостоятельно
     dropship_by_seller as (
         select datasource_id as id
         from shops_web.feature
         where feature_type= 1016
           and status is not null and status!='REVOKE'
     )
select supplier.id,
       case
           when coalesce(fulfillment.id, 0) <> 0 then 1
           else 0
           end
           as fulfillment,
       case
           when coalesce(crossdock.id, 0) <> 0 then 1
           else 0
           end
           as crossdock,
       case
           when coalesce(dropship.id, 0) <> 0 then 1
           else 0
           end
           as dropship,
       case
           when coalesce(click_and_collect.id, 0) <> 0 then 1
           else 0
           end
           as click_and_collect,
       case
           when coalesce(dropship_by_seller.id, 0) <> 0 then 1
           else 0
           end
           as dropship_by_seller
from (
        select supplier.id from shops_web.supplier
        union all
        select id from shops_web.datasource
        where id in (
            select datasource_id
            from shops_web.feature
            where feature_type=1016
              and status is not null and status!='REVOKE'
        )) supplier
         left join fulfillment
                   on supplier.id = fulfillment.id
         left join crossdock
                   on supplier.id = crossdock.id
         left join dropship
                   on supplier.id = dropship.id
         left join click_and_collect
                   on supplier.id = click_and_collect.id
         left join dropship_by_seller
                   on supplier.id = dropship_by_seller.id
