--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_ff_service_link (
	partner_id bigint not null,
	service_id bigint not null,
	feed_id bigint,
	partner_feed_id_drp bigint
) ;
create index i_parffserlin_serid on shops_web.partner_ff_service_link (service_id);
alter table shops_web.partner_ff_service_link add constraint i_parffserlin_parid_serid unique (partner_id,service_id);
create index i_parffserlin_parfeeiddrp on shops_web.partner_ff_service_link (partner_feed_id_drp);
alter table shops_web.partner_ff_service_link add constraint i_parffserlin_feeid unique (feed_id);
alter table shops_web.partner_ff_service_link add constraint fk_parffserlin_serid_delser_id foreign key (service_id) references shops_web.delivery_services(id);
alter table shops_web.partner_ff_service_link add constraint fk_parffserlin_parid_par_id foreign key (partner_id) references shops_web.partner(id);
