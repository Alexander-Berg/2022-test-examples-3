create schema sch12;

create table sch12.table01
(
  id1 int,
  id2 varchar(40),
  data varchar(40),
  constraint pk_sch_tab_id1_id2 primary key (id1, id2)
);

create unique index i_sch_tab_id1 on sch12.table01(id1);

create table sch12.table02
(
  id varchar(30),
  table01_id1 int,
  constraint fk_sch_tab_tabid1_sch_tab_id1 foreign key (table01_id1) references sch12.table01 (id1),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch12.table03
(
  table02_id varchar(30),
  constraint fk_sch_tab_tabid_sch_tab_id foreign key (table02_id) references sch12.table02 (id)
);
