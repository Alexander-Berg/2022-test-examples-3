--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-v-shops-need-info-for-test-view
create view shops_web.v_shops_need_info_for_test as
with params as (
    select entity_id                                                                         datasource_id,
           min(case when typ is not distinct from 45 then "value"::numeric end)                       is_online,
           count(case
                     when typ is not distinct from 44
                         then (select count(1) from shops_web.regions where id = p.value::numeric and typ = 3) end)
                                                                                             home_region,
           count(case
                     when typ is not distinct from 21 then
                         (select count(1)
                          from shops_web.regions
                          where id = p.value::numeric and typ = 6) end)                      local_dlv_region,
           count(case when typ is not distinct from 3 then 1 end)                            datasource_domain,
           count(case when typ is not distinct from 83 then 1 end)                           return_delivery_address,
           coalesce(min(case when typ is not distinct from 93 then "value"::numeric end), 1) cpc_enabled,
           coalesce(min(case when typ is not distinct from 81 then "value"::numeric end), 0) is_global
    from (
             select entity_id, param_type_id typ, coalesce(num_value::text, str_value) "value"
             from shops_web.param_value
             where param_type_id in (45, 44, 21, 2, 3, 81, 83, 93)
         ) p
    group by entity_id),
     outlets as (
         select datasource_id,
                sign(count(case
                               when status is not distinct from 3 then 1
                               when status is not distinct from 1 then 1
                    end)) can_publish
         from shops_web.outlet_info
         where status in (0, 1, 3)
           and hidden = 0
         group by datasource_id
     ),
     fails as (
         select distinct p.id                                                               datasource_id,
                         prm.is_online,
                         case when prm.is_online is not null then 0 else 1 end              noline,
                         case when prm.home_region is not distinct from 0 then 1 else 0 end home_region,
                         case
                             when prm.is_global = 0 then
                                 case
                                     when is_online is not distinct from 1
                                         then case
                                                  when prm.local_dlv_region is not distinct from 0 then 1
                                                  else 0
                                         end
                                     else 0 end
                             else 0
                             end                                                            local_dlv_region,
                         case
                             when cpc_enabled = 1 then
                                 case
                                     when is_online is not distinct from 1
                                         then case
                                                  when prm.datasource_domain is not distinct from 0 then 1
                                                  else 0
                                         end
                                     else 0 end
                             else 0
                             end                                                            datasource_domain,
                         case
                             when prm.is_global = 0 then case
                                                             when prm.return_delivery_address is not distinct from 0
                                                                 then 1
                                                             else 0 end
                             else 0
                             end                                                            return_delivery_address,
                         case
                             when is_online is not distinct from 1
                                 then (case when oi.datasource_id is not null then 1 else 0 end)
                             else 0 end                                                     org_info,
                         case
                             when is_online is not distinct from 1 then coalesce(oi.ogrn_is_bad, 0)
                             else 0 end                                                     ogrn_is_bad,
                         case
                             when is_online is not distinct from 1
                                 then case when si.datasource_id is not null then 1 else 0 end
                             else 0 end                                                     ship_info,
                         case
                             when is_online is not distinct from 0
                                 then case when o.datasource_id is not null then 0 else 1 end
                             else 0 end                                                     outlet,
                         case
                             when is_online is not distinct from 0 then case
                                                                            when coalesce(o.can_publish, 0) is not distinct from 0
                                                                                then 1
                                                                            else 0 end
                             else 0 end                                                     outlet_to_publish
         from shops_web.partner p
                  left join market_billing.v_current_campaign vcc on (p.id = vcc.datasource_id)
                  left join params prm on (p.id = prm.datasource_id)
                  left join shops_web.v_bad_org_info_for_test oi on (p.id = oi.datasource_id)
                  left join shops_web.v_bad_shipping_info si on (p.id = si.datasource_id)
                  left join outlets o on (p.id = o.datasource_id)
         where p.type = 'SHOP'
     ),
     res as (
         select datasource_id,
                noline,
                home_region,
                local_dlv_region,
                0                                                                     dlv_regions,
                datasource_domain,
                case when ogrn_is_bad is not distinct from 0 then org_info else 0 end org_info,
                ogrn_is_bad,
                ship_info,
                outlet,
                outlet_to_publish,
                0                                                                     contact,
                return_delivery_address
         from fails
         where is_online is null
            or home_region = 1
            or local_dlv_region = 1
            or org_info = 1
            or ship_info = 1
            or outlet = 1
            or outlet_to_publish = 1
            or datasource_domain = 1
            or return_delivery_address = 1
     )
select "datasource_id",
       "noline",
       "home_region",
       "local_dlv_region",
       "dlv_regions",
       "datasource_domain",
       "org_info",
       "ogrn_is_bad",
       "ship_info",
       "outlet",
       "outlet_to_publish",
       "contact",
       "return_delivery_address"
from res
