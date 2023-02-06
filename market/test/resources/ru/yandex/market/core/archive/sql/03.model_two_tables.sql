create schema sch03;

create table sch03.first_table
(
    id   int,
    data varchar(50),
    constraint pk_sch_firtab_id primary key (id)
);

create table sch03.second_table
(
    id             int,
    data           varchar(50),
    first_table_id int,
    constraint pk_sch_sectab_id primary key (id),
    constraint fk_sch_sectab_firtabid_sch_firtab_id foreign key (first_table_id) references sch03.first_table (id)
)
