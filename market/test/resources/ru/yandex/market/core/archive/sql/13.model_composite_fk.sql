create schema sch13;

create table sch13.table01
(
  id1 varchar(40),
  id2 int,
  data varchar(20),
  constraint pk_sch_tab1_id2_id1 primary key (id1, id2)
);

create table sch13.table02
(
  id int,
  table01_id1 varchar(40),
  table01_id2 int,
  constraint fk_sch_tab_tabid1_tabid2_sch_tab_id2_id1 foreign key (table01_id1, table01_id2) references sch13.table01 (id1, id2),
  constraint pk_sch_tab_id primary key (id)
);

create table sch13.table03
(
  id1 int,
  id2 int,
  table02_id int,
  table01_id1 varchar(40),
  table01_id2 int,
  constraint pk_sch_tab3_id2_id1 primary key (id1, id2),
  constraint fk_sch_tab_tabid_sch_tab_id foreign key (table02_id) references sch13.table02 (id),
  constraint fk_sch_tab_tabid2_tabid1_sch_tab_id2_id1 foreign key (table01_id1, table01_id2) references sch13.table01 (id1, id2)
);

create table sch13.table04
(
  table03_id1 int,
  table03_id2 int,
  constraint fk_sch_tab_tabid2_tabid1_sch_tab_id1_id2 foreign key (table03_id1, table03_id2) references sch13.table03 (id1, id2)
);
