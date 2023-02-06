--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.categories (
	id bigint,
	parent_id bigint,
	hyper_id bigint,
	category varchar(100),
	not_used smallint,
	no_search smallint,
	type varchar(16),
	accept_partner_models smallint,
	accept_partner_skus smallint,
	accept_good_content smallint not null default 0,
	published smallint not null default 0,
	accept_white_content smallint not null default 0,
	freeze_partner_content varchar(16)
) ;
create index i_cat_parid on shops_web.categories (parent_id);
alter table shops_web.categories add constraint i_cat_id unique (id);
alter table shops_web.categories add constraint i_cat_hypid unique (hyper_id);
alter table shops_web.categories add constraint fk_cat_parid_cat_id foreign key (parent_id) references shops_web.categories(id);
