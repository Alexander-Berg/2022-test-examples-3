create schema sch02;

create table sch02.first_table
(
  id   int,
  data varchar(50),
  constraint pk_sch_firtab_id primary key (id)
);

create table sch02.second_table
(
  id   int,
  data varchar(50),
  constraint pk_sch_sectab_id primary key (id)
);
