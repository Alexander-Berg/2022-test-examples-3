--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.business_service (
	business_id bigint not null,
	service_id bigint not null,
	service_type varchar(50) not null,
	constraint pk_busser_serid primary key (service_id)
) ;
create index i_busser_busid on shops_web.business_service (business_id);
alter table shops_web.business_service add constraint fk_busser_serid_par_id foreign key (service_id) references shops_web.partner(id);
alter table shops_web.business_service add constraint fk_busser_busid_bus_id foreign key (business_id) references shops_web.business(id) on delete cascade not deferrable initially immediate;
