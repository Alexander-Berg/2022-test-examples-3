create schema if not exists WMS;

create table if not exists WMS.ITRNSERIAL
(
    SERIALKEY        int auto_increment primary key,
    WHSEID           nvarchar(30)   default 'test',
    ITRNSERIALKEY    nvarchar(10)                  not null unique,
    ITRNKEY          nvarchar(10)                  not null,
    STORERKEY        nvarchar(15)   default ''     not null,
    SKU              nvarchar(50)   default ''     not null,
    LOT              nvarchar(10)   default ''     not null,
    ID               nvarchar(50)   default ''     not null,
    LOC              nvarchar(10)   default ''     not null,
    SERIALNUMBER     nvarchar(30)                  not null,
    QTY              decimal(22, 5) default 0      not null,
    TRANTYPE         nvarchar(30),
    DATA2            nvarchar(30),
    DATA3            nvarchar(30),
    DATA4            nvarchar(30),
    DATA5            nvarchar(30),
    GROSSWEIGHT      decimal(22, 5) default 0,
    NETWEIGHT        decimal(22, 5) default 0,
    SERIALNUMBERLONG nvarchar(500),
    ADDDATE          datetime       default now()  not null,
    ADDWHO           nvarchar(256)  default 'test' not null,
    EDITDATE         datetime       default now()  not null,
    EDITWHO          nvarchar(256)  default 'test' not null
    );

create table if not exists WMS.TASKACTIONLOG
(
    UserName      nvarchar(30) default 'test' not null,
    EventType     int                         not null,
    OperationType nvarchar(30)                not null,
    EventDate     datetime     default now()  not null,
    Location      nvarchar(30) default 'test' not null,
    primary key (UserName, EventType, OperationType, EventDate)
    );
