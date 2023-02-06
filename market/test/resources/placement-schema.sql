CREATE SCHEMA IF NOT EXISTS placement;

CREATE TABLE IF NOT EXISTS placement.PLACEMENTORDER
(
    PLACEMENTORDERKEY  INT IDENTITY,
    ASSIGNEE           NVARCHAR(256)                     not null,
    STATUS             NVARCHAR(15)  default 'NEW'       not null,
    STARTDATE          DATETIME2,
    ENDDATE            DATETIME2,
    ADDDATE            DATETIME2     default now()       not null,
    EDITDATE           DATETIME2     default now()       not null,
    ADDWHO             NVARCHAR(256) default 'test'      not null,
    EDITWHO            NVARCHAR(256) default 'test'      not null,
    ORDER_TYPE         NVARCHAR(15)  default 'PLACEMENT' not null,
    FASTZONE_HINT      nvarchar(10),
    BESTSELLER_PERCENT decimal(22, 5),
    PUTAWAYZONE        nvarchar(10),
    WITH_CHECK         NVARCHAR(10)
);

CREATE TABLE IF NOT EXISTS placement.PLACEMENTORDERxID
(
    SERIALKEY         INT IDENTITY,
    PLACEMENTORDERKEY INT                                NOT NULL,
    ID                NVARCHAR(63)                       not null,
    STATUS            NVARCHAR(15)  default 'NOT_PLACED' not null,
    ADDSTAGE          NVARCHAR(15)                       not null,
    ADDDATE           DATETIME2     default now()        not null,
    EDITDATE          DATETIME2     default now()        not null,
    ADDWHO            NVARCHAR(256) default 'test'       not null,
    EDITWHO           NVARCHAR(256) default 'test'       not null,
    ACTIVE            BIT           default 0            not null,
    PARENT_ID         NVARCHAR(50)  default null
);

ALTER TABLE placement.PLACEMENTORDERxID
    ADD
        CONSTRAINT IF NOT EXISTS FK_PLCMENTORDERKEY_PLCMNTRDRKY FOREIGN KEY (PLACEMENTORDERKEY)
            REFERENCES placement.PLACEMENTORDER (PLACEMENTORDERKEY);

CREATE TABLE IF NOT EXISTS placement.PLACEMENTORDERxSERIALNUMBER
(
    SERIALKEY         INT IDENTITY,
    PLACEMENTORDERKEY INT                                not null,
    SERIALNUMBER      NVARCHAR(63)                       not null,
    STATUS            NVARCHAR(15)  default 'NOT_PLACED' not null,
    ID                NVARCHAR(63)                       not null,
    ADDSTAGE          NVARCHAR(15)                       not null,
    ADDDATE           DATETIME2     default now()        not null,
    EDITDATE          DATETIME2     default now()        not null,
    ADDWHO            NVARCHAR(256) default 'test'       not null,
    EDITWHO           NVARCHAR(256) default 'test'       not null,
    FROM_LOC          NVARCHAR(10)
);

ALTER TABLE placement.PLACEMENTORDERxSERIALNUMBER
    ADD
        CONSTRAINT IF NOT EXISTS FK_PLCMNTORDRxSRLNMBR_PLCMNT_RDRKY FOREIGN KEY (PLACEMENTORDERKEY)
            REFERENCES placement.PLACEMENTORDER (PLACEMENTORDERKEY);

CREATE TABLE IF NOT EXISTS placement.PLACEMENTLOG
(
    SERIALKEY         INT IDENTITY,
    PLACEMENTORDERKEY INT                          NOT NULL,
    SERIALNUMBER      NVARCHAR(50)                 NOT NULL,
    ID                NVARCHAR(50)                 NOT NULL,
    LOC               NVARCHAR(10)                 NOT NULL,
    PLACEDATE         DATETIME2     default now()  not null,
    ADDDATE           DATETIME2     default now()  not null,
    EDITDATE          DATETIME2     default now()  not null,
    ADDWHO            NVARCHAR(256) default 'test' not null,
    EDITWHO           NVARCHAR(256) default 'test' not null,
    PLACEMENT_TYPE    NVARCHAR(10)
);

ALTER TABLE placement.PLACEMENTLOG
    ADD
        CONSTRAINT IF NOT EXISTS FK_PLCMNTLG_RDRKY FOREIGN KEY (PLACEMENTORDERKEY)
            REFERENCES placement.PLACEMENTORDER (PLACEMENTORDERKEY);

CREATE TABLE IF NOT EXISTS placement.SHEDLOCK
(
    name       VARCHAR(64)  NOT NULL,
    lock_until datetime2    NOT NULL,
    locked_at  datetime2    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

create table if not exists placement.JOB_HISTORY
(
    ID         int identity primary key,
    JOB_NAME   nvarchar(200) not null,
    START_TIME datetime2     not null,
    END_TIME   datetime2,
    STATUS     nvarchar(15),
    MESSAGE    nvarchar(max),
    REQUEST_ID nvarchar(400)
);

create table if not exists placement.RECOMMENDED_ROW
(
    ID                int identity,
    PLACEMENTORDERKEY int                          not null,
    ROW_ID            nvarchar(10)                 not null,
    PRIORITY          int                          not null,
    ADDDATE           datetime2     default now()  not null,
    EDITDATE          datetime2     default now()  not null,
    ADDWHO            nvarchar(256) default 'test' not null,
    EDITWHO           nvarchar(256) default 'test' not null,

    primary key (id),
    constraint IDX_RECOMMENDED_ROW_ORDERKEY_ROWID unique (PLACEMENTORDERKEY, ROW_ID)
);

create table if not exists placement.RECOMMENDED_ROW
(
    ID                int identity,
    PLACEMENTORDERKEY int                          not null,
    ROW_ID            nvarchar(10)                 not null,
    PRIORITY          int                          not null,
    ADDDATE           datetime2     default now()  not null,
    EDITDATE          datetime2     default now()  not null,
    ADDWHO            nvarchar(256) default 'test' not null,
    EDITWHO           nvarchar(256) default 'test' not null,

    primary key (id),
    constraint IDX_RECOMMENDED_ROW_ORDERKEY_ROWID unique (PLACEMENTORDERKEY, ROW_ID)
);

CREATE SCHEMA IF NOT EXISTS archive;

CREATE TABLE IF NOT EXISTS archive.PLACEMENTORDER
(
    PLACEMENTORDERKEY  INT                          not null,
    ASSIGNEE           NVARCHAR(256)                not null,
    STATUS             NVARCHAR(15)  default 'NEW'  not null,
    STARTDATE          DATETIME2,
    ENDDATE            DATETIME2,
    ADDDATE            DATETIME2     default now()  not null,
    EDITDATE           DATETIME2     default now()  not null,
    ADDWHO             NVARCHAR(256) default 'test' not null,
    EDITWHO            NVARCHAR(256) default 'test' not null,
    ORDER_TYPE         nvarchar(15)                 not null default 'PLACEMENT',
    FASTZONE_HINT      nvarchar(10),
    BESTSELLER_PERCENT decimal(22, 5),
    PUTAWAYZONE        nvarchar(10),
    WITH_CHECK         NVARCHAR(10)
);

CREATE TABLE IF NOT EXISTS archive.PLACEMENTORDERxID
(
    SERIALKEY         INT                                not null,
    PLACEMENTORDERKEY INT                                NOT NULL,
    ID                NVARCHAR(63)                       not null,
    STATUS            NVARCHAR(15)  default 'NOT_PLACED' not null,
    ADDSTAGE          NVARCHAR(15)                       not null,
    ADDDATE           DATETIME2     default now()        not null,
    EDITDATE          DATETIME2     default now()        not null,
    ADDWHO            NVARCHAR(256) default 'test'       not null,
    EDITWHO           NVARCHAR(256) default 'test'       not null,
    ACTIVE            bit                                not null default 0,
    PARENT_ID         NVARCHAR(50)  default null
);

CREATE TABLE IF NOT EXISTS archive.PLACEMENTORDERxSERIALNUMBER
(
    SERIALKEY         INT                                not null,
    PLACEMENTORDERKEY INT                                not null,
    SERIALNUMBER      NVARCHAR(63)                       not null,
    STATUS            NVARCHAR(15)  default 'NOT_PLACED' not null,
    ID                NVARCHAR(63)                       not null,
    ADDSTAGE          NVARCHAR(15)                       not null,
    ADDDATE           DATETIME2     default now()        not null,
    EDITDATE          DATETIME2     default now()        not null,
    ADDWHO            NVARCHAR(256) default 'test'       not null,
    EDITWHO           NVARCHAR(256) default 'test'       not null,
    FROM_LOC          nvarchar(10)
);

CREATE TABLE IF NOT EXISTS archive.PLACEMENTLOG
(
    SERIALKEY         INT                          not null,
    PLACEMENTORDERKEY INT                          NOT NULL,
    SERIALNUMBER      NVARCHAR(50)                 NOT NULL,
    ID                NVARCHAR(50)                 NOT NULL,
    LOC               NVARCHAR(10)                 NOT NULL,
    PLACEDATE         DATETIME2     default now()  not null,
    ADDDATE           DATETIME2     default now()  not null,
    EDITDATE          DATETIME2     default now()  not null,
    ADDWHO            NVARCHAR(256) default 'test' not null,
    EDITWHO           NVARCHAR(256) default 'test' not null,
    PLACEMENT_TYPE    NVARCHAR(10)
);
