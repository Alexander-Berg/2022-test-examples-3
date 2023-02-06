create schema sch07;

create table sch07.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch07.table02
(
  id int,
  table01_id1 int,
  table01_id2 int,
  constraint fk_sch_tab_tabid1_sch_tab_id foreign key (table01_id1) references sch07.table01 (id),
  constraint fk_sch_tab_tabid2_sch_tab_id foreign key (table01_id2) references sch07.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);
