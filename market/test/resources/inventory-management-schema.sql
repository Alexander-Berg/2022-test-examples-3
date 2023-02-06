create schema if not exists inventory_management;

create sequence if not exists inventory_management.SEQ_TASK_GROUP;

create table if not exists inventory_management.TASK
(
    id                bigint identity,
    status            nvarchar(50)  default 'NEW'        not null,
    type              nvarchar(50)                       not null,
    source            nvarchar(50)                       not null,
    putawayzone       nvarchar(10)                       not null,
    loc               nvarchar(10)                       not null,
    locationtype      nvarchar(10)                       not null,
    group_id          int                                not null,
    by_qty            bit           default 1            not null,
    external_task_id  bigint,
    execute_who       nvarchar(256),
    has_discrepancies bit           default 0            not null,
    invent_date       datetime,
    add_date          datetime      default now()        not null,
    add_who           nvarchar(256) default 'test_user'  not null,
    edit_date         datetime      default now()        not null,
    edit_who          nvarchar(256) default 'test_user'  not null,

    primary key (ID)
);
