--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.datafeed (
	is_enabled smallint not null default 0,
	url varchar(1024),
	login varchar(50),
	password varchar(50),
	datasource_id bigint not null,
	id bigint,
	upload_id bigint,
	reparse_interval_minutes numeric,
	site_type smallint not null,
	is_default smallint not null default 0,
	upload_template_type varchar(40)
) ;
alter table shops_web.datafeed add constraint c_dat_isdef_url check ((is_default = 0 and url is not null) or (is_default = 1 and url is null));
create index i_dat_uplid on shops_web.datafeed (upload_id);
alter table shops_web.datafeed add constraint i_dat_id unique (id);
create index i_dat_datid on shops_web.datafeed (datasource_id);
create unique index i_dat_isdef_datid on shops_web.datafeed ((case when is_default=1 then datasource_id else null end));
alter table shops_web.datafeed add constraint fk_dat_datid_par_id foreign key (datasource_id) references shops_web.partner(id) not valid;
