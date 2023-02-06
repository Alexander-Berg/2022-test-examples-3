--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.environment (
	name text not null,
	value text not null,
	constraint pk_env_nam_val primary key (name, value)
) ;
