--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.timezone (
	id bigint not null,
	name varchar(50) not null,
	time_offset numeric not null,
	constraint pk_tim_id primary key (id)
) ;
alter table shops_web.timezone add constraint i_tim_nam unique (name);
