--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.updated_mplace_program (
	datasource_id bigint not null,
	supplier_id bigint not null,
	program_type varchar(50) not null,
	added_at timestamp with time zone not null default current_timestamp,
	feeds_imported smallint not null default 0,
	constraint pk_updmplpro_datid primary key (datasource_id)
) ;
