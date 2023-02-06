create schema if not exists dbo;

create table if not exists dbo.job_monitoring_config
(
    job_group varchar(200),
    job_name VARCHAR(256) NOT NULL
        CONSTRAINT job_monitoring_config_pkey
            PRIMARY KEY,
    max_delay_time INTEGER NOT NULL,
    warn_delay_time INTEGER NOT NULL,
    max_execution_time INTEGER NOT NULL,
    warn_execution_time INTEGER NOT NULL,
    max_failed_runs INTEGER NOT NULL,
    warn_failed_runs INTEGER NOT NULL,
    runs_number_to_consider_for_hanging INTEGER NOT NULL,
    runs_number_to_consider_for_failing INTEGER NOT NULL,
    tracking_start_time     datetime default now() not null,
    max_execution_runs INTEGER NOT NULL,
    warn_execution_runs INTEGER NOT NULL
);

create table if not exists dbo.QRTZ_FIRED_TRIGGERS
(
    SCHED_NAME varchar(120) not null,
    ENTRY_ID varchar(95) not null,
    TRIGGER_NAME varchar(200) not null,
    TRIGGER_GROUP varchar(200) not null,
    INSTANCE_NAME varchar(200) not null,
    FIRED_TIME bigint not null,
    SCHED_TIME bigint not null,
    PRIORITY int not null,
    STATE varchar(16) not null,
    JOB_NAME varchar(200),
    JOB_GROUP varchar(200),
    IS_NONCONCURRENT bit,
    REQUESTS_RECOVERY bit,
    primary key (SCHED_NAME, ENTRY_ID)
);

create table if not exists dbo.QRTZ_JOB_DETAILS
(
    SCHED_NAME        varchar(120) not null,
    JOB_NAME          varchar(200) not null,
    JOB_GROUP         varchar(200) not null,
    DESCRIPTION       varchar(250),
    JOB_CLASS_NAME    varchar(250) not null,
    IS_DURABLE        bit          not null,
    IS_NONCONCURRENT  bit          not null,
    IS_UPDATE_DATA    bit          not null,
    REQUESTS_RECOVERY bit          not null,
    JOB_DATA          varbinary(max),
    primary key (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE if not exists dbo.CONFIG_PROPERTIES
(
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(4000),
    add_who nvarchar(256) default 'test' not null,
    edit_who nvarchar(256) default 'test' not null,
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
    valid_from DATETIME NOT NULL ,
    value NVARCHAR(MAX),
    add_who nvarchar(256) default 'test' not null,
    edit_who nvarchar(256) default 'test' not null
);

create table if not exists dbo.CONFIG_PROPERTIES_HISTORY
(
    id         int identity
        constraint PK_ConfigPropertiesHistory
            primary key,
    name       nvarchar(100) not null,
    value      nvarchar(max),
    commentary nvarchar(4000),
    add_who    nvarchar(256) default 'test' not null,
    adddate    datetime not null
);

create TABLE if not exists dbo.QRTZ_HISTORY
(
    ID             int identity,
    JOB_NAME       nvarchar(200) not null,
    JOB_GROUP      nvarchar(200) not null,
    JOB_CLASS_NAME nvarchar(250) not null,
    STARTTIME      datetime      not null,
    ENDTIME        datetime,
    STATUS         nvarchar,
    MESSAGE        nvarchar(max),
    request_id     nvarchar(400)
);

CREATE ALIAS IF NOT EXISTS GETUTCDATE AS
'java.util.Date getDate() {
return new java.util.Date();
}';
