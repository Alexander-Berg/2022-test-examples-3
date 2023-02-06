--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.feature_open_cutoff (
	id bigint not null default nextval('shops_web.s_cutoff'),
	datasource_id bigint not null,
	feature_type bigint not null,
	cutoff_type bigint not null,
	from_time timestamp with time zone not null default current_timestamp,
	"COMMENT" text,
	requires_moderation smallint,
	restricts_indexation smallint,
	reason text,
	user_id bigint,
	message text,
	update_time timestamp with time zone,
	constraint pk_feaopecut_id primary key (id)
) ;
alter table shops_web.feature_open_cutoff add constraint i_feaopecut_datid_featyp_cuttyp unique (datasource_id, feature_type, cutoff_type);
alter table shops_web.feature_open_cutoff add constraint fk_feaopecut_datid_par_id foreign key (datasource_id) references shops_web.partner(id);
