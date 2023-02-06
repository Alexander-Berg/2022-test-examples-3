create schema sch15;

create table sch15.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch15.table02
(
  id int,
  parent_id int,
  table01_id int,
  constraint fk_sch_tab_parid_sch_tab_id foreign key (parent_id) references sch15.table02 (id),
  constraint fk_sch_tab_tabid_sch_tab_id1 foreign key (table01_id) references sch15.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);
