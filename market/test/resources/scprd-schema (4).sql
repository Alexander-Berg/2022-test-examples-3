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
