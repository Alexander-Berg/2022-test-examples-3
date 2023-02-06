--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_app_meta (
	id bigint not null default nextval('shops_web.s_partner_app_meta'),
	creation_time timestamp(6) with time zone not null,
	type varchar(100) not null,
	update_date timestamp(6) with time zone not null,
	signatory_date timestamp(6) with time zone default null,
	market_id bigint,
	constraint pk_parappmet_id primary key (id)
) ;
