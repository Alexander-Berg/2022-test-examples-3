--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.regions (
                                   id bigint not null,
                                   name varchar(100) not null,
                                   parent_region_id bigint,
                                   type smallint,
                                   tz_offset int,
                                   tz_name varchar(50),
                                   tz_id bigint,
                                   longitude decimal(19,12),
                                   latitude decimal(19,12),
                                   constraint pk_reg_id primary key (id)
) ;
create index i_reg_tznam on shops_web.regions (tz_name);
create index i_reg_tzid on shops_web.regions (tz_id);
create index i_reg_parregid on shops_web.regions (parent_region_id);
alter table shops_web.regions add constraint fk_reg_tzid_tim_id foreign key (tz_id) references shops_web.timezone(id);
