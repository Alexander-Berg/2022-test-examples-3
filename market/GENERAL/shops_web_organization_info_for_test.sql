--liquibase formatted sql
--changeset jaguar1337:MBI-71559-create-shops-web-organization-info-for-test-view
create view shops_web.organization_info_for_test as
select info_id,
       datasource_id,
       type,
       name,
       juridical_address,
       ogrn,
       information_source,
       info_group_id,
       registration_number,
       info_url
from (select last_info.info_id,
             last_info.datasource_id,
             i.type,
             i.name,
             i.juridical_address,
             i.ogrn,
             i.information_source,
             i.info_group_id,
             i.registration_number,
             i.info_url
      from (select info_id,
                   datasource_id,
                   ctid                                                                               rwid,
                   row_number() over (partition by datasource_id, info_group_id order by info_id desc) rn
            from shops_web.organization_info_all
           ) last_info,
           shops_web.organization_info_all i,
           shops_web.organization_info_group oig
      where last_info.rn = 1
        and last_info.rwid = i.ctid
        and oig.info_group_id = i.info_group_id
        and oig.is_deleted = 0
     ) as lii

