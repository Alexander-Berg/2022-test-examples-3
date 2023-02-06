--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_app_contact (
	datasource_id bigint,
	request_id bigint,
	contact_person text,
	phone_number varchar(100),
	email varchar(100),
	shop_phone_number varchar(100),
	shop_contact_address text,
	contact_first_name varchar(512),
	contact_middle_name varchar(512),
	contact_last_name varchar(512)
) ;
