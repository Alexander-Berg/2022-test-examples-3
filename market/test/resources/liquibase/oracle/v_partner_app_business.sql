--liquibase formatted sql
--changeset stani:test-ora
create view shops_web.v_partner_app_business
            (
             datasource_id, request_id, date_logged,
             org_type, inn, fact_address, contact_person, phone_number, email, account_num, bik, org_name, prepay_type,
             status, seller_client_id, updated_at, ogrn, jur_address, corr_acc_num, "COMMENT", license_num,
             license_date, signatory, signatory_doc_type, signatory_doc_info, bank_name, work_schedule, kpp, postcode,
             start_date, person_id, contract_id, request_type, shop_phone_number, signatory_position, signatory_gender,
             market_id, shop_contact_address, contact_first_name, contact_middle_name, contact_last_name,
             signatory_first_name, signatory_middle_name, signatory_last_name, onboarding_contact_id,
             fact_addr_region_id)
as
(
select pa.partner_id      as datasource_id,
       pa.request_id,
       pam.creation_time  as date_logged,
       pab.org_type,
       pab.inn,
       pab.fact_address,
       pac.contact_person,
       pac.phone_number,
       pac.email,
       pab.account_num,
       pab.bik,
       pab.org_name,
       pab.prepay_type,
       case pa.status
           when 'INIT' then 0
           when 'IN_PROGRESS' then 1
           when 'COMPLETED' then 2
           when 'FROZEN' then 3
           when 'CLOSED' then 4
           when 'DECLINED' then 5
           when 'INTERNAL_CLOSED' then 6
           when 'NEW' then 7
           when 'NEED_INFO' then 8
           when 'CANCELLED' then 9
           when 'NEW_PROGRAMS_VERIFICATION_REQUIRED' then 10
           when 'NEW_PROGRAMS_VERIFICATION_FAILED' then 11
           end            as status,
       pabd.seller_client_id,
       pam.update_date    as updated_at,
       pab.ogrn,
       pab.jur_address,
       pab.corr_acc_num,
       pa.abo_comment     as "COMMENT",
       pab.license_num,
       pab.license_date,
       pab.signatory,
       pab.signatory_doc_type,
       pab.signatory_doc_info,
       pab.bank_name,
       pab.work_schedule,
       pab.kpp,
       pab.postcode,
       pam.signatory_date as start_date,
       pab.person_id,
       pab.contract_id,
       pam.type           as request_type,
       pac.shop_phone_number,
       pab.signatory_position,
       pab.signatory_gender,
       pam.market_id,
       pac.shop_contact_address,
       pac.contact_first_name,
       pac.contact_middle_name,
       pac.contact_last_name,
       pab.signatory_first_name,
       pab.signatory_middle_name,
       pab.signatory_last_name,
       pab.onboarding_contact_id,
       pab.fact_addr_region_id
from shops_web.partner_app_meta pam
         join shops_web.partner_app pa on pam.id = pa.request_id
         join shops_web.partner_app_business pab on pa.request_id = pab.request_id and pa.partner_id = pab.datasource_id
         left join shops_web.partner_app_balance_data pabd on pam.id = pabd.request_id
         left join shops_web.partner_app_contact pac
                   on pac.request_id = pab.request_id and pac.datasource_id = pab.datasource_id
where pa.status != 'INTERNAL_CLOSED');

--changeset a-chmil:MBI-67669-grant-select-v_partner_app_business
grant select on shops_web.v_partner_app_business to public;
