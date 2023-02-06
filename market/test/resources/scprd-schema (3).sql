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

create table if not exists wmwhse1.LOGISTIC_UNIT
(
    SERIALKEY int auto_increment,
    WHSEID nvarchar(30),
    RECEIPTKEY nvarchar(10) not null,
    STORERKEY nvarchar(15) default '' not null,
    SKU nvarchar(50) null,
    EXTERN_ORDER_KEY nvarchar(50),
    EXTERN_RECEIPT_KEY nvarchar(50),
    REGISTER_KEY nvarchar(50),
    UNIT_KEY nvarchar(50) not null,
    PARENT_UNIT_KEY nvarchar(50),
    TYPE nvarchar(10) not null,
    STATUS nvarchar(50) not null,
    COUNT decimal(22, 5) default 0 not null,
    MAX_RECEIPT_DATE datetime,
    BOXES_IN_ORDER decimal(10),
    SHOULD_BE_RECEIPT bit,
    ADDDATE datetime not null,
    ADDWHO nvarchar(256) not null,
    EDITDATE datetime not null,
    EDITWHO nvarchar(256) not null,
    STOCKTYPE nvarchar(50),
    primary key (SERIALKEY),
    constraint order_key_unit_key_unique_constraint unique (EXTERN_ORDER_KEY, UNIT_KEY)
);


create table if not exists wmwhse1.ITRN
(
    SERIALKEY int auto_increment,
    WHSEID nvarchar(30) default 'test',
    ITRNKEY nvarchar(10) not null primary key,
    ITRNSYSID int,
    TRANTYPE nvarchar(10) not null,
    STORERKEY nvarchar(15) not null,
    SKU nvarchar(50) not null,
    LOT nvarchar(10) not null,
    FROMLOC nvarchar(10) not null,
    FROMID nvarchar(50) not null,
    TOLOC nvarchar(10) not null,
    TOID nvarchar(50) not null,
    SOURCEKEY nvarchar(20),
    SOURCETYPE nvarchar(30),
    STATUS nvarchar(10),
    LOTTABLE01 nvarchar(50) default '' not null,
    LOTTABLE02 nvarchar(50) default '' not null,
    LOTTABLE03 nvarchar(50) default '' not null,
    LOTTABLE04 datetime,
    LOTTABLE05 datetime,
    LOTTABLE06 nvarchar(50) default '' not null,
    LOTTABLE07 nvarchar(50) default '' not null,
    LOTTABLE08 nvarchar(50) default '' not null,
    LOTTABLE09 nvarchar(50) default '' not null,
    LOTTABLE10 nvarchar(50) default '' not null,
    CASECNT decimal(22,5) default 0 not null,
    INNERPACK decimal(22,5) default 0 not null,
    QTY decimal(22,5) default 0 not null,
    PALLET decimal(22,5) default 0 not null,
    CUBE float default 0 not null,
    GROSSWGT decimal(22,5) default 0 not null,
    NETWGT decimal(22,5) default 0 not null,
    OTHERUNIT1 float default 0 not null,
    OTHERUNIT2 float default 0 not null,
    PACKKEY nvarchar(50),
    UOM nvarchar(10),
    UOMCALC int,
    UOMQTY decimal(22,5),
    EFFECTIVEDATE datetime default now() not null,
    RECEIPTKEY nvarchar(10),
    RECEIPTLINENUMBER nvarchar(10),
    HOLDCODE nvarchar(10),
    LOTTABLE11 datetime,
    LOTTABLE12 datetime,
    COUNTSEQUENCE int default 0 not null,
    TAREWGT decimal(22,5) default 0 not null,
    FINALTOLOC nvarchar(20),
    INTRANSIT nvarchar(1) default '1',
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null
);

create table if not exists wmwhse1.ITRNSERIAL
(
    SERIALKEY int auto_increment primary key,
    WHSEID nvarchar(30) default 'test',
    ITRNSERIALKEY nvarchar(10) not null unique,
    ITRNKEY nvarchar(10) not null references ITRN,
    STORERKEY nvarchar(15) default '' not null,
    SKU nvarchar(50) default '' not null,
    LOT nvarchar(10) default '' not null,
    ID nvarchar(50) default '' not null,
    LOC nvarchar(10) default '' not null,
    SERIALNUMBER nvarchar(30) not null,
    QTY decimal(22,5) default 0 not null,
    TRANTYPE nvarchar(30),
    DATA2 nvarchar(30),
    DATA3 nvarchar(30),
    DATA4 nvarchar(30),
    DATA5 nvarchar(30),
    GROSSWEIGHT decimal(22,5) default 0,
    NETWEIGHT decimal(22,5) default 0,
    SERIALNUMBERLONG nvarchar(500),
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null
);

create table if not exists wmwhse1.ERRLOG
(
    SERIALKEY int not null,
    WHSEID nvarchar(30) default 'wmwhse1',
    LOGDATE datetime default '2020-01-01 18:55:15.000',
    USERID nvarchar(256) default 'testUser',
    ERRORID int auto_increment,
    SYSTEMSTATE nvarchar(30),
    MODULE nvarchar(30),
    ADDDATE datetime default '2020-01-01 18:55:15.000',
    ADDWHO nvarchar(256) default 'wmwhse1',
    EDITDATE datetime not null,
    EDITWHO nvarchar(256) default 'wmwhse1',
    ERRORTEXT nvarchar(MAX) default 'test'
);

create table if not exists wmwhse1.WP_Eventlog
(
    EVENTID int primary key not null,
    EVENTTIME datetime,
    EVENTTYPE nvarchar(256) default 'test',
    DESCRIPTION nvarchar(1024) default 'test',
    EVENTTYPENUM int,
    USERID nvarchar(256) default 'testUser',
    WHSEID nvarchar(30) default 'wmwhse1',
    EVENTDETAILS nvarchar(1024) default 'test'
);

create table if not exists wmwhse1.BACKGROUNDJOBSTATUS
(
    SERIALKEY int not null,
    PROCESSHANDLEID nvarchar(255) primary key,
    THEPROCNAME nvarchar(255) default 'NSPRELEASEWAVE',
    JOBSTATUS nvarchar(1) default '4',
    USERID nvarchar(256) default 'testUser',
    STARTTIME datetime default '2020-01-01 18:55:15.000',
    ENDTIME datetime not null
);

create table if not exists  wmwhse1.TRANSMITLOG
(
    SERIALKEY           int
    unique,
    WHSEID              nvarchar(30)  default 'test',
    TRANSMITLOGKEY      nvarchar(10)                       not null
    primary key,
    TABLENAME           nvarchar(30)  default ''           not null,
    KEY1                nvarchar(50)  default ''           not null,
    KEY2                nvarchar(50)  default ''           not null,
    KEY3                nvarchar(50)  default ''           not null,
    KEY4                nvarchar(50)  default ''           not null,
    KEY5                nvarchar(50)  default ''           not null,
    TRANSMITFLAG        nvarchar(5)   default '0'          not null,
    TRANSMITFLAG2       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG3       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG4       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG5       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG6       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG7       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG8       nvarchar(5)   default '0'          not null,
    TRANSMITFLAG9       nvarchar(5)   default '0'          not null,
    BILLINGTRANSMITFLAG nvarchar(5)   default '0'          not null,
    LABORTRANSMITFLAG   nvarchar(5)   default '0'          not null,
    TMTRANSMITFLAG      nvarchar(5)   default '0'          not null,
    TRANSMITBATCH       nvarchar(10)  default ''           not null,
    EVENTSTATUS         int           default 0            not null,
    EVENTFAILURECOUNT   int           default 0            not null,
    EVENTCATEGORY       nvarchar      default 'E'          not null,
    MESSAGE             nvarchar(max),
    ADDDATE             datetime,
    ADDWHO              nvarchar(256) default 'test'  not null,
    EDITDATE            datetime,
    EDITWHO             nvarchar(256) default 'test'  not null
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
    REQUEST_ID        nvarchar(128),
    SERIALKEY         int unique
);

create table if not exists wmwhse1.vlotexpired
(
    LOT nvarchar(10),
    QTY decimal(22,5)
);
