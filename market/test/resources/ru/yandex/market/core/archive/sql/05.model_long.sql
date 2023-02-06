create schema sch05;

create table sch05.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch05.table02
(
  id         int,
  table01_id int,
  constraint fk_sch_tab2_tabid_sch_tab_id foreign key (table01_id) references sch05.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);

create table sch05.table03
(
  id         int,
  table02_id int,
  constraint fk_sch_tab3_tabid_sch_tab_id foreign key (table02_id) references sch05.table02 (id),
  constraint pk_sch_tab3_id primary key (id)
);

create table sch05.table04
(
  id         int,
  table03_id int,
  constraint fk_sch_tab4_tabid_sch_tab_id foreign key (table03_id) references sch05.table03 (id),
  constraint pk_sch_tab4_id primary key (id)
);

create table sch05.table05
(
  id         int,
  table02_id int,
  constraint fk_sch_tab5_tabid_sch_tab_id foreign key (table02_id) references sch05.table02 (id),
  constraint pk_sch_tab5_id primary key (id)
);
