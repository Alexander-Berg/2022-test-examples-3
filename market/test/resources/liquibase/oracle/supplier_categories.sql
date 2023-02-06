--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.supplier_categories (
	supplier_id bigint not null,
	category_id bigint not null
) ;
create index i_supcat_supid_catid on shops_web.supplier_categories (supplier_id, category_id);
create index i_supcat_catid on shops_web.supplier_categories (category_id);
