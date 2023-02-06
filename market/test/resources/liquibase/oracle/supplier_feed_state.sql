--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.supplier_feed_state (
	fid bigint not null,
	gid numeric not null,
	return_code smallint not null,
	published_session varchar(20),
	release_time timestamp with time zone,
	noffers numeric,
	total_offers numeric,
	feed_processing_type varchar(10),
	business_id bigint,
	is_united_catalog smallint,
	constraint pk_supfeesta_fid primary key (fid)
) ;
