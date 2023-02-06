--liquibase formatted sql
--changeset jaguar1337:MBI-70471-create-shops-web-datasources-in-testing-table
create table shops_web.datasources_in_testing (
	id bigint not null,
	datasource_id bigint,
	ready smallint,
	approved smallint,
	in_progress smallint,
	cancelled smallint,
	push_ready_count bigint,
	fatal_cancelled smallint,
	iter_count numeric,
	updated_at timestamp with time zone,
	recommendations text,
	start_date timestamp with time zone,
	testing_type smallint not null default 0,
	claim_link bigint not null default 0,
	status smallint not null default 0,
	attempt_num bigint not null default 0,
	quality_check_required smallint not null default 0,
	clone_check_required smallint not null default 0,
	shop_program varchar(25) not null,
	constraint pk_datintes_id primary key (id)
) ;
comment on table shops_web.datasources_in_testing is e'shopoffer::магазины на премодерации
таблица описывает состояние магазинов на премодерации. она содержит набор флагов, отвечающих за различные аспекты магазина. не все комбинации флагов допустимы.';
comment on column shops_web.datasources_in_testing.in_progress is e'флаг того, что магазин находится на премодерации ';
comment on column shops_web.datasources_in_testing.clone_check_required is e'необходимость пройти проверку на клоновость';
comment on column shops_web.datasources_in_testing.fatal_cancelled is e'флаг того, что процесс премодерации отменен и не может быть возобновлен нажатием кнопки в партнерском интерфейсе ';
comment on column shops_web.datasources_in_testing.id is e'идентификатор записи в таблице ';
comment on column shops_web.datasources_in_testing.shop_program is e'тип программы. cpc, cpc и т.д.';
comment on column shops_web.datasources_in_testing.push_ready_count is e'количество нажатий на кнопку "начать проверку" в партнерской части интерфейса ';
comment on column shops_web.datasources_in_testing.cancelled is e'флаг того, что процесс премодерации отменен (в результате ошибок магазина) ';
comment on column shops_web.datasources_in_testing.approved is e'флаг того, что магазин допущен к премодерации ';
comment on column shops_web.datasources_in_testing.datasource_id is e'идентификатор магазина, для которого сохранено состояние ';
comment on column shops_web.datasources_in_testing.quality_check_required is e'необходимость пройти проверку на качество';
comment on column shops_web.datasources_in_testing.ready is e'флаг того, что магазин к премодерации ';
comment on column shops_web.datasources_in_testing.status is e'статус магазина в тестинге (см. testingstatus.java)';
comment on column shops_web.datasources_in_testing.attempt_num is e'общее кол-во попыток со штрафом и без него';
alter table shops_web.datasources_in_testing add constraint c_datintes_shopro check (shop_program in ('CPC', 'CPA', 'SELF_CHECK', 'GENERAL', 'API_DEBUG'));
alter table shops_web.datasources_in_testing add constraint i_datintes_datid_shopro unique (datasource_id,shop_program);
alter table shops_web.datasources_in_testing add constraint fk_datintes_datid_dat_id foreign key (datasource_id) references shops_web.datasource(id);
