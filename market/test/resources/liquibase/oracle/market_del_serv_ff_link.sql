--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.market_del_serv_ff_link (
	id bigint not null,
	from_ff_id bigint,
	to_ff_id bigint,
	return_ff_id bigint,
	to_logistics_point_id bigint,
	shipment_type varchar(32),
	cutoff_time_hour smallint,
	constraint pk_mardelserfflin_id primary key (id)
) ;
