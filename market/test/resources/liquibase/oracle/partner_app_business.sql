--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_app_business (
	business_id bigint,
	datasource_id bigint,
	request_id bigint,
	org_type smallint,
	inn varchar(100),
	fact_address text,
	account_num varchar(100),
	bik varchar(100),
	org_name varchar(512),
	prepay_type smallint,
	ogrn varchar(40),
	jur_address text,
	corr_acc_num varchar(100),
	license_num varchar(100),
	license_date timestamp with time zone,
	signatory varchar(512),
	signatory_doc_type smallint,
	signatory_doc_info varchar(512),
	bank_name varchar(512),
	work_schedule varchar(512),
	kpp varchar(40),
	postcode varchar(40),
	person_id bigint,
	contract_id bigint,
	signatory_position varchar(512),
	signatory_gender varchar(2),
	signatory_first_name varchar(512),
	signatory_middle_name varchar(512),
	signatory_last_name varchar(512),
	onboarding_contact_id bigint,
	fact_addr_region_id bigint
) ;