create schema if not exists wmwhse1;

create table if not exists wmwhse1.SERIALINVENTORY
(
    SERIALKEY          int,
    STORERKEY          nvarchar(15)   default ''           not null,
    SKU                nvarchar(50)   default ''           not null,
    LOT                nvarchar(10)   default ''           not null,
    ID                 nvarchar(50)   default ''           not null,
    LOC                nvarchar(10)   default ''           not null,
    SERIALNUMBER       nvarchar(30)                        not null,
    ADDDATE            datetime,
    ADDWHO             nvarchar(256),
    EDITDATE           datetime,
    EDITWHO            nvarchar(256),
    IS_FAKE            nvarchar       default '0'          not null
);

create table if not exists wmwhse1.TASK_ROUTER_NOTIFICATION_QUEUE_V2
(
    SERIALKEY int identity
        unique,
    topic     nvarchar(256)   default ''                      not null,
    receiver  nvarchar(256)   default ''                      not null,
    message   nvarchar(max)   default ''                      not null,
    actions   nvarchar(max)   default ''                      not null,
    sender    nvarchar(256)   default ''                      not null,
    node_name nvarchar(256),
    add_date  datetime      default '' not null,
    add_who   nvarchar(256) default ''  not null
);
