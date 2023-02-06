create schema if not exists dbo;

create table if not exists dbo.PROCESSEXECHISTORY
(
    SERIALKEY         int not null,
    INITIALPROCESSHID nvarchar(255),
    PROCESSHANDLEID   nvarchar(255) not null,
    USERID            nvarchar(256),
    SESSIONID         nvarchar(255),
    PROCESSNAME       nvarchar(255),
    ISBATCH           nvarchar,
    STARTTIME         datetime2,
    ENDTIME           datetime2,
    TENANTID          nvarchar(255),
    NODEID            nvarchar(255),
    COMPONENTID       nvarchar(255),
    FACILITY          nvarchar(255),
    LOADFACTOR        decimal(14, 2)
    );

create table if not exists dbo.PROCESSEXECATTR
(
    SERIALKEY       int not null,
    PROCESSHANDLEID nvarchar(255)  not null ,
    ATTRNAME        nvarchar(255),
    ATTRVALUE       nvarchar(2000) default 'test'
);

create table if not exists dbo.PROCESSEXECATTRHISTORY
(
    SERIALKEY       int,
    PROCESSHANDLEID nvarchar(255),
    ATTRNAME        nvarchar(255),
    ATTRVALUE       nvarchar(2000)
);

create table if not exists dbo.PROCESSEXEC
(
    SERIALKEY         int not null,
    INITIALPROCESSHID nvarchar(255),
    PROCESSHANDLEID   nvarchar(255) not null
        primary key,
    USERID            nvarchar(256),
    SESSIONID         nvarchar(255),
    PROCESSNAME       nvarchar(255),
    ISBATCH           nvarchar,
    STARTTIME         datetime2,
    ENDTIME           datetime2,
    TENANTID          nvarchar(255),
    NODEID            nvarchar(255),
    COMPONENTID       nvarchar(255),
    FACILITY          nvarchar(255),
    LOADFACTOR        decimal(14, 2)
)
