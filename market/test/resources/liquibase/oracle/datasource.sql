--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.datasource (
          id bigint not null,
          name varchar(200),
          created_at timestamp with time zone,
          comments text,
          manager_id bigint not null,
          constraint pk_dat_id primary key (id)
) ;
alter table shops_web.datasource add constraint fk_dat_id_par_id foreign key (id) references shops_web.partner(id);

