create schema sch21;

create table sch21.table01
(
  id int,
  data int
);

create unique index i_sch_tab_id on sch21.table01 (id);

create table sch21.table02
(
  id int,
  table01_id int,
  constraint fk_sch_tab_tabid_sch_tab_id1 foreign key (table01_id) references sch21.table01 (id),
  constraint pk_sch_tab2_id primary key (id)
);
