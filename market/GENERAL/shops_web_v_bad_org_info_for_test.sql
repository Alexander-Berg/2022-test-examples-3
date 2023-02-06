--liquibase formatted sql
--changeset zhebelenkoga:MBI-79807

create or replace view shops_web.v_bad_org_info_for_test(datasource_id, ogrn_is_bad) as
select id, max(ogrn_is_bad) ogrn_is_bad
from (
         select ds.id,
                oi.name,
                oi.ogrn,
                oi.type,
                oi.juridical_address,
                oi.registration_number,
                case
                    when oi.information_source = 1 then 0
                    when oi.ogrn is not null or oi.registration_number is not null then
                        case
                            when coalesce(oi.ogrn, registration_number) !~ concat('^', oic.ogrn, '$')
                                then 1
                            when oic.is_russia = 1 and oi.ogrn is not null and shops_web.check_ogrn(oi.ogrn) = 0
                                then 1
                            when oic.is_belarus = 1 and oi.ogrn is not null and shops_web.check_unp(oi.ogrn) = 0
                                then 1
                            else 0
                            end
                    else 0
                    end ogrn_is_bad
         from shops_web.datasource ds
                  left join shops_web.organization_info_for_test oi on ds.id = oi.datasource_id
                  inner join shops_web.v_org_info_constrains oic on ds.id = oic.shop_id
         where oi.type is null
            or oic.if_type = oi.type
     ) as doo
where ogrn_is_bad = 1
   or name is null
   or coalesce(ogrn, registration_number) is null
   or (type in (1, 2, 6) and juridical_address is null)
group by id

