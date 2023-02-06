create schema sch06;

create table sch06.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch06.table02
(
  id int,
  table01_id int,
  constraint fk_sch_tab_tabid_sch_tab_id foreign key (table01_id) references sch06.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch06.table03
(
  id         int,
  table01_id int,
  constraint fk_sch_tab3_tabid_sch_tab_id foreign key (table01_id) references sch06.table01 (id),
  constraint pk_sch_tab3_id primary key (id)
)
