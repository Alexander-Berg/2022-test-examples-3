create schema sch17;

create table sch17.table01
(
  id int,
  data varchar(40),
  constraint pk_sch_tab1_id primary key (id)
);

create table sch17.param_type
(
  param_type_id int,
  entity_name varchar(40),
  constraint pk_sch_partyp_partypid primary key (param_type_id)
);

create table sch17.param_value
(
  param_value_id int,
  param_type_id int,
  entity_id int,
  data varchar(40),
  constraint pk_sch_parval_parvalid primary key (param_value_id),
  constraint fk_sch_parval_partypid_sch_partyp_partypid foreign key (param_type_id) references sch17.param_type(param_type_id)
);

create table sch17.table04
(
  param_value_id int,
  constraint fk_sch_tab_parvalid_sch_parval_parvalid foreign key (param_value_id) references sch17.param_value(param_value_id)
)
