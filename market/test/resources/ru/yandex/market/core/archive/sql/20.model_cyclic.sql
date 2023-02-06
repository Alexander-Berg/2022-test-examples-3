create schema sch20;

create table sch20.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch20.table02
(
  id int,
  table01_id int,
  table03_id int,
  constraint fk_sch_tab_tabid_sch_tab_id1 foreign key (table01_id) references sch20.table01(id),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch20.table03
(
  id int,
  table01_id int,
  table02_id int,
  constraint fk_sch_tab_tab1id_sch_tab_id foreign key (table01_id) references sch20.table01(id),
  constraint fk_sch_tab_tab2id_sch_tab_id foreign key (table02_id) references sch20.table02(id),
  constraint pk_sch_tab3_id primary key (id)
);

alter table sch20.table02 add constraint fk_sch_tab_tabid_sch_tab_id foreign key (table03_id) references sch20.table03(id);

