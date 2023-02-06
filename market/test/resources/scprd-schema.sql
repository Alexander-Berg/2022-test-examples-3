create schema if not exists wmwhse1;

create table if not exists wmwhse1.NSQLCONFIG
(
    SERIALKEY int auto_increment,
    WHSEID nvarchar(30) default 'test',
    CONFIGKEY nvarchar(100) not null primary key,
    NSQLVALUE nvarchar(140) default '',
    NSQLDEFAULT nvarchar(30) default '' not null,
    NSQLDESCRIP nvarchar(120),
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null,
    PARAMETERTYPE nvarchar(1) default 'F' not null,
    MODIFIABLE nvarchar(1) default '1' not null,
    EFFECTIVE nvarchar(1) default 'I' not null,
    CATEGORY nvarchar(10)
);

create table if not exists wmwhse1.TASKMANAGERUSER
(
    SERIALKEY            int auto_increment,
    WHSEID               nvarchar(30)  default 'auth',
    USERKEY              nvarchar(256) default ''           not null primary key,
    PRIORITYTASKTYPE     nvarchar(10)  default '1'          not null,
    STRATEGYKEY          nvarchar(10)  default ''           not null,
    EQUIPMENTPROFILEKEY  nvarchar(10)  default ''           not null,
    LASTCASEIDPICKED     nvarchar(50)  default ''           not null,
    LASTWAVEKEY          nvarchar(10)  default ''           not null,
    TTMSTRATEGYKEY       nvarchar(10)  default 'STD',
    LASTLOC              nvarchar(10)  default ''           not null,
    LASTTOLOC            nvarchar(30),
    USR_NAME             nvarchar(256),
    USR_STATUS           int           default 1,
    USR_FNAME            nvarchar(256),
    USR_LNAME            nvarchar(256),
    TASKMANAGERUSERID    nvarchar(32),
    HOURLYRATE           decimal(9, 3) default 0.0,
    BILLABLEWAGE         decimal(9, 3),
    DEFAULTLABOROWNER    nvarchar(15),
    PAYROLLAPPROVALGROUP nvarchar(20),
    ADDDATE              datetime      default '2021-09-03 11:33:39.733' not null,
    USERGROUP            nvarchar(10)  default ' '          not null,
    ADDWHO               nvarchar(256) default 'auth'  not null,
    EDITDATE             datetime      default '2021-09-03 11:33:39.733' not null,
    EDITWHO              nvarchar(256) default 'auth'  not null,
    IS_OUTSTAFF          boolean,
    IS_NEWBIE            boolean       default 0 not null,
    GENDER               nvarchar(10),
    STAFF_LOGIN          nvarchar(256)
);

create schema if not exists enterprise;

create table if not exists enterprise.MOBILE_PROFILE
(
    PROFILE_NAME nvarchar(256) not null primary key,
    STDPROD      bit           not null
);

create table if not exists enterprise.MOBILE_USER
(
    USER_NAME    nvarchar(256) not null primary key,
    PROFILE_NAME nvarchar(256) not null references enterprise.MOBILE_PROFILE
);

create table if not exists wmwhse1.AUTH_AUDIT_DETAIL
(
    EVENT_TYPE        nvarchar(32),
    SUCCESSFUL        bit,
    EVENT_DATE_TIME   datetime default '2022-02-22 04:00:00.000' not null,
    EVENT_ATTRIBUTES  nvarchar(1024),
    USERNAME          nvarchar(256),
    AFFECTED_USERNAME nvarchar(256),
    DEVICE_ID         nvarchar(128),
    IP_ADDRESS        nvarchar(64),
    REQUEST_ID        nvarchar(128)
);

create table if not exists wmwhse1.DEVICE_AUDIT_DETAIL
(
    SERIALKEY         int auto_increment,
    EVENT_DATE_TIME   datetime default '2022-02-22 04:00:00.000' not null,
    USERNAME          nvarchar(256) not null unique,
    DEVICE_ID         nvarchar(128) not null unique
);

CREATE TABLE if not exists wmwhse1.BEGINNER_USER (
    USER_NAME            nvarchar(256)   NOT NULL PRIMARY KEY,
    END_TIME             datetime,
    CONSTRAINT FK_BEGINNER_USER_USER_NAME FOREIGN KEY (USER_NAME) REFERENCES wmwhse1.TASKMANAGERUSER(USERKEY)
);

CREATE INDEX if not exists IDX_BEGINNER_USER_END_TIME ON wmwhse1.BEGINNER_USER (END_TIME)
