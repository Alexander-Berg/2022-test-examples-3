create schema sch16;

create table sch16.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch16.table02
(
  id int,
  parent_id int,
  table01_id int,
  constraint fk_sch_tab_parid_sch_tab_id foreign key (parent_id) references sch16.table02 (id),
  constraint fk_sch_tab_tabid_sch_tab_id1 foreign key (table01_id) references sch16.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch16.table03
(
  table02_id int,
  constraint fk_sch_tab_tabid_sch_tab_id foreign key (table02_id) references sch16.table02 (id)
)
