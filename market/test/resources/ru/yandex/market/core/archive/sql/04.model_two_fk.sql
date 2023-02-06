create schema sch04;

create table sch04.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch04.table02
(
  id int,
  constraint pk_sch_tab2_id primary key (id)
);

create table sch04.table03
(
  id         int,
  table01_id int,
  table02_id int,
  constraint fk_sch_tab1_tabid_sch_tab_id foreign key (table01_id) references sch04.table01 (id),
  constraint fk_sch_tab2_tabid_sch_tab_id foreign key (table02_id) references sch04.table02 (id),
  constraint pk_sch_tab3_id primary key (id)
)
