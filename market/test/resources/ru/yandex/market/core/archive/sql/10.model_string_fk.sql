create schema sch10;

create table sch10.table01
(
  id varchar(40),
  data int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch10.table02
(
  id int,
  table01_id varchar(40),
  constraint fk_sch_tab2_tabid_sch_tab_id foreign key (table01_id) references sch10.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch10.table03
(
  id varchar(40),
  data int,
  constraint pk_sch_tab3_id primary key (id)
);

create table sch10.table04
(
  id int,
  table03_id varchar(40),
  constraint fk_sch_tab4_tabid_sch_tab_id foreign key (table03_id) references sch10.table03 (id),
  constraint pk_sch_tab4_id primary key (id)
);
