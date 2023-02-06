--liquibase formatted sql

--changeset dpastukhov:MBI-62672_create-organization_info_for_test
create or replace view shops_web.organization_info_for_test as
select INFO_ID,
       DATASOURCE_ID,
       TYPE,
       NAME,
       JURIDICAL_ADDRESS,
       OGRN,
       INFORMATION_SOURCE,
       INFO_GROUP_ID,
       REGISTRATION_NUMBER,
       INFO_URL
from (select last_info.INFO_ID,
             last_info.DATASOURCE_ID,
             i.TYPE,
             i.NAME,
             i.JURIDICAL_ADDRESS,
             i.OGRN,
             i.INFORMATION_SOURCE,
             i.INFO_GROUP_ID,
             i.REGISTRATION_NUMBER,
             i.INFO_URL
      from (select info_id,
                   datasource_id,
                   rowid rwid,
                   row_number() over (partition by datasource_id, info_group_id order by info_id desc) rn
            from SHOPS_WEB.ORGANIZATION_INFO_ALL
           ) last_info,
           SHOPS_WEB.ORGANIZATION_INFO_ALL i,
           SHOPS_WEB.organization_info_group oig
      where last_info.rn = 1
            and last_info.rwid = i.rowid
            and oig.info_group_id = i.info_group_id
            and oig.is_deleted = 0
     );

--changeset dpastukhov:MBI-62672_create-v_bad_org_info_for_test
create or replace view shops_web.v_bad_org_info_for_test (
   datasource_id,
   ogrn_is_bad
)
as
select id, max(ogrn_is_bad) ogrn_is_bad
from
  (
    select ds.id, oi.name, oi.ogrn, oi.type, oi.juridical_address, oi.registration_number,
      case
        when oi.information_source = 1 /*YA_MONEY*/ then 0
        when (oi.ogrn is not null or oi.registration_number is not null) and
          case
            when regexp_instr(nvl(oi.ogrn, registration_number), '^' || oic.ogrn || '$') = 0 then 1
            when oic.is_russia = 1 and oi.ogrn is not null and shops_web.check_ogrn(oi.ogrn) = 0 then 1
            when oic.is_belarus = 1 and oi.ogrn is not null and shops_web.check_unp(oi.ogrn) = 0 then 1
          end = 1 then 1
        else 0
      end ogrn_is_bad
    from shops_web.datasource ds
      left join shops_web.organization_info_for_test oi on ds.id = oi.datasource_id
      inner join shops_web.v_org_info_constrains oic on ds.id = oic.shop_id
    where oi.type is null or oic.if_type = oi.type
  )
  where ogrn_is_bad = 1 or name is null or nvl(ogrn, registration_number) is null or (type in (1,2,6) and juridical_address is null)
  group by id
;

--changeset storabic:MBI-65316-remove-superadmin
CREATE OR REPLACE VIEW shops_web.v_shops_need_info_for_test (
  datasource_id,
  noline,
  home_region,
  local_dlv_region,
  dlv_regions,
  datasource_domain,
  org_info,
  ogrn_is_bad,
  ship_info,
  outlet,
  outlet_to_publish,
  contact,
  return_delivery_address
)
AS
with params as (
  select entity_id datasource_id,
    min(decode(typ, 45, value)) is_online,
    count(decode(typ, 44, (select count(1) from regions where id=p.value and typ=3))) home_region,
    count(decode(typ, 21, (select count(1) from regions where id=p.value and typ=6))) local_dlv_region,
    count(decode(typ, 3, 1)) datasource_domain,
    count(decode(typ, 83, 1)) return_delivery_address,
    nvl(min(decode(typ, 93, value)), 1) cpc_enabled,
    nvl(min(decode(typ, 81, value)), 0) is_global
  from
  (
    select entity_id, param_type_id typ, nvl(to_char(num_value), str_value) value
    from param_value where param_type_id in (45, 44, 21, 2, 3, 81, 83, 93)
   ) p
  group by entity_id ),
 outlets as (
  select datasource_id, sign(count(decode(status, 3, 1, 1, 1))) can_publish
  from shops_web.outlet_info
  where status in (0,1,3) and hidden = 0
  group by datasource_id
),
fails as (
  select DISTINCT p.id datasource_id,
    prm.is_online,
    nvl2(prm.is_online, 0, 1) noline,
    decode(prm.home_region, 0, 1, 0) home_region,
    case when prm.is_global = 0 then
      decode(is_online, 1, decode(prm.local_dlv_region, 0, 1, 0), 0)
      else 0
    end local_dlv_region,
    case when cpc_enabled = 1 then
      decode(is_online, 1, decode(prm.datasource_domain, 0, 1, 0), 0)
      else 0
    end datasource_domain,
    case
        when prm.is_global = 0 then decode(prm.return_delivery_address, 0, 1, 0)
        else 0
    end return_delivery_address,
    decode(is_online, 1, nvl2(oi.datasource_id, 1, 0), 0) org_info,
    decode(is_online, 1, nvl(oi.ogrn_is_bad, 0), 0) ogrn_is_bad,
    decode(is_online, 1, nvl2(si.datasource_id, 1, 0), 0) ship_info,
    decode(is_online, 0, nvl2(o.datasource_id, 0, 1), 0) outlet,
    decode(is_online, 0, decode(nvl(o.can_publish, 0), 0, 1, 0), 0) outlet_to_publish
  from
    partner p
        left join market_billing.v_current_campaign vcc on (p.id = vcc.datasource_id)
        left join params prm on (p.id = prm.datasource_id)
        left join v_bad_org_info_for_test oi on (p.id = oi.datasource_id)
        left join v_bad_shipping_info si on (p.id = si.datasource_id)
        left join outlets o on (p.id = o.datasource_id)
    where p.type = 'SHOP'
),
res as (
  select
    datasource_id, noline, home_region, local_dlv_region, 0 dlv_regions, datasource_domain,
    decode(ogrn_is_bad, 0, org_info, 0) org_info, ogrn_is_bad,
    ship_info, outlet, outlet_to_publish, 0 contact, return_delivery_address
  from
    fails
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
select * from res;
