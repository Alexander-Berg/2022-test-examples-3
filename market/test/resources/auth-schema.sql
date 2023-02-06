create schema if not exists dbo;

create table if not exists auth.ACCESS_BY_ROLES
(
    TYPE   nvarchar(100) not null,
    CODE   nvarchar(100) not null,
    ROLE   nvarchar(100) not null,
    ACCESS nvarchar(100) not null,
    primary key (CODE, ROLE)
);

create table if not exists auth.SCREEN
(
    SCREEN     nvarchar(100)    not null
    primary key,
    DESCR    nvarchar(100)      not null,
    ADDDATE  datetime,
    ADDWHO   nvarchar(256),
    EDITDATE datetime,
    EDITWHO  nvarchar(256),
    ACCESS   nvarchar(100),
    COMMENT nvarchar(256)
)
