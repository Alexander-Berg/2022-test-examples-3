--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.param_value (
                                       param_value_id bigint not null,
                                       param_type_id bigint not null,
                                       entity_id bigint not null,
                                       num bigint not null default 1,
                                       num_value numeric,
                                       str_value text,
                                       date_value timestamp with time zone,
                                       constraint pk_parval_parvalid primary key (param_value_id)
) ;
create index i_parval_partypid on shops_web.param_value (param_type_id);
alter table shops_web.param_value add constraint i_parval_entid_partypid_num unique (entity_id,param_type_id,num);
