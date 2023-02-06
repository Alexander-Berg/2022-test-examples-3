create schema if not exists reporter;
create schema if not exists dbo;

create table if not exists reporter.rpt_task(
    UID BIGINT NOT NULL IDENTITY PRIMARY KEY,
    CODE nvarchar(100),
    PARAMS nvarchar(max),
    STATUS nvarchar(20),
    ADDDATE datetime default now() not null,
    ADDWHO NVARCHAR(256) NOT NULL,
    MESSAGE NVARCHAR(MAX)
);

create table if not exists reporter.rpt_data(
    UID BIGINT,
    DATA nvarchar(max)
);

create table if not exists reporter.rpt_link(
    UID         bigint identity
        primary key,
    SOURCE_KEY  nvarchar(50)                       not null,
    SOURCE_TYPE nvarchar(50)                       not null,
    LINK        nvarchar(max)                      not null,
    ADDDATE     datetime      default now() not null,
    ADDWHO      nvarchar(256) default 'test'  not null,
    NAME        nvarchar(max)
);

CREATE TABLE  if not exists dbo.CONFIG_PROPERTIES
(
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(4000),
    add_who nvarchar(256) default 'test',
    edit_who nvarchar(256) default 'test',
    value       nvarchar(max),
    category    nvarchar(10),
    commentary  nvarchar(4000),
    adddate     datetime not null,
    editdate    datetime not null
);

CREATE TABLE if not exists dbo.CONFIG_PROPERTY_VALUES
(
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    valid_from DATETIME2 NOT NULL ,
    value NVARCHAR(MAX),
    add_who nvarchar(256) default 'test',
    edit_who nvarchar(256) default 'test'
);

create table if not exists dbo.CONFIG_PROPERTIES_HISTORY
(
    id         int identity
    constraint PK_ConfigPropertiesHistory
    primary key,
    name       nvarchar(100) not null,
    value     nvarchar(max),
    commentary nvarchar(4000),
    add_who    nvarchar(256) default 'test' not null,
    adddate    datetime not null
);
