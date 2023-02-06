--liquibase formatted sql
--changeset jaguar1337:MBI-70471-create-shops-web-test-generation-and-delta-table
create table shops_web.test_generation_and_delta (
	id bigint not null,
	name varchar(64) not null,
	start_time timestamp with time zone not null,
	end_time timestamp with time zone not null,
	release_time timestamp with time zone not null,
	mitype varchar(100),
	sc_version varchar(16) not null,
	type smallint default 0,
	is_imported smallint not null default 1
) ;
create unique index i_tesgenanddel_nam_typ on shops_web.test_generation_and_delta ((case type when 0 then name end));
create index i_tesgenanddel_reltim on shops_web.test_generation_and_delta (release_time);
create index i_tesgenanddel_id on shops_web.test_generation_and_delta (id);
