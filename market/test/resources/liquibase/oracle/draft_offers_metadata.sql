--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.draft_offers_metadata (
	supplier_id bigint not null,
	offers_count numeric not null,
	constraint pk_draoffmet_supid primary key (supplier_id)
) ;
