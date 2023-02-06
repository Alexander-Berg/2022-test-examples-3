--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.feature (
	id bigint not null default nextval('shops_web.feature_seq'),
	datasource_id bigint not null,
	feature_type bigint not null,
	status varchar(255) not null,
	constraint pk_fea_id primary key (id)
) ;
alter table shops_web.feature add constraint i_fea_datid_featyp unique (datasource_id, feature_type);
alter table shops_web.feature add constraint fk_fea_datid_par_id foreign key (datasource_id) references shops_web.partner(id);
