--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_app_balance_data (
	request_id bigint not null,
	seller_client_id bigint not null,
	balance_firm varchar(50) not null default 'yandex_europe_ag'
) ;
alter table shops_web.partner_app_balance_data add constraint i_parappbaldat_reqid unique (request_id);
alter table shops_web.partner_app_balance_data add constraint fk_parappbaldat_reqid_parappmet_id foreign key (request_id) references shops_web.partner_app_meta(id);
