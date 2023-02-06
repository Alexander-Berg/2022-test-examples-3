--liquibase formatted sql
--changeset boldinegor:MBI-63031-partners
create table shops_web.partner (
   id bigint not null default nextval('shops_web.s_datasource'),
   type varchar(20) not null,
   manager_id bigint,
   constraint pk_par_id primary key (id)
) ;
