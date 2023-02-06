create schema sch22;

create table sch22.table01
(
  id int,
  constraint pk_sch_tab1_id primary key (id)
);

create table sch22.table02
(
  id         int,
  table01_id int,
  constraint fk_sch_tab_tabid_sch_tab_id foreign key (table01_id) references sch22.table01 (id) on delete cascade,
  constraint pk_sch_tab2_id primary key (id)
);
