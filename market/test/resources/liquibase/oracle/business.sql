--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.business (
	id bigint not null default nextval('shops_web.s_datasource'),
	name varchar(512) not null,
	created_at timestamp(6) with time zone not null default current_timestamp,
	campaign_id bigint not null default nextval('market_billing.s_campaign_info'),
	client_id_always_null bigint,
	is_deleted smallint,
	constraint pk_bus_id primary key (id)
) ;
create index i_bus_cliidalwnul on shops_web.business (client_id_always_null);
alter table shops_web.business add constraint i_bus_camid unique (campaign_id);
alter table shops_web.business add constraint fk_bus_id_par_id foreign key (id) references shops_web.partner(id);
create index i_bus_nam on shops_web.business ((lower(trim(both name))));
