--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.delivery_services (
	id bigint not null,
	url varchar(128),
	human_readable_id varchar(255),
	pickup_available smallint,
	is_common smallint,
	description text,
	logo text,
	market_status varchar(64),
	balance_client_id bigint,
	date_switch_hour numeric,
	type varchar(64) not null,
	rating numeric,
	name varchar(256) not null,
	is_express smallint,
	source smallint,
	external_id varchar(100),
	address text,
	shipment_type varchar(100),
	settlement varchar(100),
	is_dropoff smallint,
	constraint pk_delser_id primary key (id)
) ;
