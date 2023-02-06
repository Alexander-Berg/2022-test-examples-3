--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_app (
	request_id bigint not null,
	partner_id bigint not null,
	status varchar(100) not null,
	abo_comment text
) ;
create index i_parapp_reqid on shops_web.partner_app (request_id);
alter table shops_web.partner_app add constraint i_parapp_parid_reqid unique (partner_id,request_id);
alter table shops_web.partner_app add constraint fk_parapp_reqid_parappmet_id foreign key (request_id) references shops_web.partner_app_meta(id);
alter table shops_web.partner_app add constraint fk_parapp_parid_par_id foreign key (partner_id) references shops_web.partner(id);
