SET MODE Oracle;

-- drop table category_parameter_values;
create table category (
  hyper_id    number not null,
  published   char(1) default 1 not null,
  constraint category_pk primary key (hyper_id)
);

create table parameter (
  id    number(14,0) not null,
  type  varchar2(12 char),
  constraint parameter_pkey primary key (id)
);

create table enum_option(
  id        number not null,
  param_id  number(14,0) not null,
  name      varchar2(1000),

  constraint enum_option_pk primary key (id)
);

create table boolean_value(
  id        number not null,
  param_id  number(14,0) not null,
  value     number(1),

  constraint boolean_value_pk primary key (id)
);

create table category_parameter_values (
  category_hid              number(19) not null
    constraint category_parameter_hid_fk
    references category,
  param_id                  number(19) not null
    constraint category_parameter_id_fk
    references parameter,
  option_id                 number(19) default null
    constraint category_parameter_option_fk
    references enum_option,
  boolean_value_id          number(19) default null
    constraint category_parameter_boolean_fk
    references boolean_value,
  values_data               blob not null
);

create index category_parameter_value_idx on category_parameter_values(category_hid);
