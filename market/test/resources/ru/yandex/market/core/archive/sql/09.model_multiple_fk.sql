create schema sch09;

create table sch09.table01
(
  "id1" int,
  "ID2" int,
  data varchar(40),
  constraint pk_sch_tab_ID2_id1 primary key ("id1", "ID2")
);

create unique index i_sch_tab_id1 on sch09.table01 ("id1");
create unique index i_sch_tab_ID2 on sch09.table01 ("ID2");

create table sch09.table02
(
  id int,
  table01_id1 int,
  table01_id2 int,
  constraint fk_sch_tab_tabid2_tabid1_sch_tab_ID2_id1 foreign key (table01_id1, table01_id2) references sch09.table01 ("id1", "ID2"),
  constraint fk_sch_tab_tabid1_sch_tab_id1 foreign key (table01_id1) references sch09.table01 ("id1"),
  constraint fk_sch_tab_tabid2_sch_tab_ID2 foreign key (table01_id2) references sch09.table01 ("ID2"),
  constraint pk_sch_tab2_id primary key (id)
);
