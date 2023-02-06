create schema if not exists constraints;

create table if not exists constraints.RULES
(
    ID          int identity,
    TITLE       nvarchar(200)                not null,
    OBJECT_TYPE nvarchar(10)                 not null,
    ADDDATE     datetime2     default now()  not null,
    EDITDATE    datetime2     default now()  not null,
    ADDWHO      nvarchar(256) default 'test' not null,
    EDITWHO     nvarchar(256) default 'test' not null,

    primary key (ID)
);

create table if not exists constraints.RULE_RESTRICTION
(
    ID        int identity,
    RULE_ID   int                          not null,
    TYPE      nvarchar(20)                 not null,
    PARAM     nvarchar(20)                 not null,
    COLLATION nvarchar(10)                 not null,
    VALUE     nvarchar(20)                 not null,
    ADDDATE   datetime2     default now()  not null,
    EDITDATE  datetime2     default now()  not null,
    ADDWHO    nvarchar(256) default 'test' not null,
    EDITWHO   nvarchar(256) default 'test' not null,

    primary key (ID),
    foreign key (RULE_ID) references constraints.RULES (ID)
);

create table if not exists constraints.RULE_GROUP
(
    ID          int identity,
    TITLE       nvarchar(200)                not null,
    OBJECT_TYPE nvarchar(10)                 not null,
    ADDDATE     datetime2     default now()  not null,
    EDITDATE    datetime2     default now()  not null,
    ADDWHO      nvarchar(256) default 'test' not null,
    EDITWHO     nvarchar(256) default 'test' not null,

    primary key (ID)
);

create table if not exists constraints.RULE_2_RULE_GROUP
(
    RULE_ID       int                          not null,
    RULE_GROUP_ID int                          not null,
    ADDDATE       datetime2     default now()  not null,
    EDITDATE      datetime2     default now()  not null,
    ADDWHO        nvarchar(256) default 'test' not null,
    EDITWHO       nvarchar(256) default 'test' not null,

    primary key (RULE_ID, RULE_GROUP_ID),
    foreign key (RULE_ID) references constraints.RULES (ID),
    foreign key (RULE_GROUP_ID) references constraints.RULE_GROUP (ID)
);

create table if not exists constraints.RANGE_GROUP
(
    ID          int identity,
    PUTAWAYZONE nvarchar(10)                 not null,
    ADDDATE     datetime2     default now()  not null,
    EDITDATE    datetime2     default now()  not null,
    ADDWHO      nvarchar(256) default 'test' not null,
    EDITWHO     nvarchar(256) default 'test' not null,

    primary key (ID)
);

create table if not exists constraints.RANGE
(
    ID             int identity,
    RANGE_GROUP_ID int                          not null,
    START_LOC      nvarchar(10)                 not null,
    END_LOC        nvarchar(10)                 not null,
    RANGE_TYPE     nvarchar(20)                 not null,
    TIER           nvarchar(10),
    ADDDATE        datetime2     default now()  not null,
    EDITDATE       datetime2     default now()  not null,
    ADDWHO         nvarchar(256) default 'test' not null,
    EDITWHO        nvarchar(256) default 'test' not null,

    primary key (ID),
    foreign key (RANGE_GROUP_ID) references constraints.RANGE_GROUP (ID)
);

create table if not exists constraints.RANGE_GROUP_2_RULE_GROUP
(
    RANGE_GROUP_ID int                          not null,
    RULE_GROUP_ID  int                          not null,
    COLLATION      nvarchar(10)                 not null,
    ADDDATE        datetime2     default now()  not null,
    EDITDATE       datetime2     default now()  not null,
    ADDWHO         nvarchar(256) default 'test' not null,
    EDITWHO        nvarchar(256) default 'test' not null,

    primary key (RANGE_GROUP_ID, RULE_GROUP_ID),
    foreign key (RANGE_GROUP_ID) references constraints.RANGE_GROUP (ID),
    foreign key (RULE_GROUP_ID) references constraints.RULE_GROUP (ID)
);

create table if not exists constraints.CARGOTYPE
(
    CODE        int primary key,
    DESCRIPTION nvarchar(100)                not null,
    ENABLED     bit           default 0      not null,
    ADDDATE     datetime2     default now()  not null,
    EDITDATE    datetime2     default now()  not null,
    ADDWHO      nvarchar(256) default 'test' not null,
    EDITWHO     nvarchar(256) default 'test' not null
);

create table if not exists constraints.CONSTRAINTS_ISSUE
(
    ID                   int identity,
    SKU                  nvarchar(50)                 not null,
    STORERKEY            nvarchar(15)                 not null,
    LOC                  nvarchar(10),
    TYPE                 nvarchar(20)                 not null,
    STATUS               nvarchar(20)  default 'NEW'  not null,
    VALUE                nvarchar(200)                not null,
    STORAGE_CATEGORY     nvarchar(30),
    STORAGE_CATEGORY_OLD nvarchar(30),
    PUTAWAYZONE          nvarchar(10),
    ADDDATE              datetime2     default now()  not null,
    EDITDATE             datetime2     default now()  not null,
    ADDWHO               nvarchar(256) default 'test' not null,
    EDITWHO              nvarchar(256) default 'test' not null,

    primary key (ID)
);

create table if not exists constraints.JOB_HISTORY
(
    ID         int identity primary key,
    JOB_NAME   nvarchar(200) not null,
    START_TIME datetime2     not null,
    END_TIME   datetime2,
    STATUS     nvarchar(15),
    MESSAGE    nvarchar(max),
    REQUEST_ID nvarchar(400)
);

create table if not exists constraints.SHEDLOCK
(
    name       VARCHAR(64)  NOT NULL primary key,
    lock_until datetime2    NOT NULL,
    locked_at  datetime2    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

create table if not exists constraints.CONFIG_PROPERTIES
(
    ID          int identity primary key,
    NAME        nvarchar(100)                not null unique,
    DESCRIPTION nvarchar(4000),
    VALUE       nvarchar(max),
    CATEGORY    nvarchar(10),
    COMMENTARY  nvarchar(4000),
    ADD_WHO     nvarchar(256) default 'test' not null,
    EDIT_WHO    nvarchar(256) default 'test' not null,
    ADDDATE     datetime      default now()  not null,
    EDITDATE    datetime      default now()  not null
);
