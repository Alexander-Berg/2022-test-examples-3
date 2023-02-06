--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.partner_placement_program (
	partner_id bigint not null,
	program varchar(100) not null,
	update_at timestamp with time zone not null default current_timestamp,
	created_at timestamp with time zone not null default current_timestamp,
	status varchar(100) not null,
	ever_activated smallint not null default 0,
	constraint pk_parplapro_parid_pro primary key (partner_id,program)
) ;
alter table shops_web.partner_placement_program add constraint fk_parplapro_parid_par_id foreign key (partner_id) references shops_web.partner(id);
