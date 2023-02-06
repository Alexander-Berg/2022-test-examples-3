create schema sch14;

create table sch14.table01
(
  id int,
  parent_id int,
  constraint fk_sch_tab_parid_sch_tab_id foreign key (parent_id) references sch14.table01 (id),
  constraint pk_sch_tab_id primary key (id)
);
