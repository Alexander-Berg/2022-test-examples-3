CREATE SCHEMA IF NOT EXISTS [WMWHSE1];

CREATE TABLE IF NOT EXISTS WMWHSE1.RECEIPTSTATUSHISTORY (
  SERIALKEY  int          NOT NULL,
  WHSEID     varchar(30),
  RECEIPTKEY varchar(10)  NOT NULL,
  STATUS     varchar(2)   NOT NULL,
  SOURCE     varchar(30)  NOT NULL,
  ADDDATE    timestamp    NOT NULL,
  ADDWHO     varchar(256) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS UNIQUE_IX
  ON wmwhse1.RECEIPTSTATUSHISTORY (SERIALKEY);


CREATE TABLE IF NOT EXISTS WMWHSE1.ORDERSTATUSHISTORY
(
  SERIALKEY       int                      NOT NULL PRIMARY KEY,
  WHSEID          varchar(30),
  ORDERKEY        varchar(10)              not null,
  ORDERLINENUMBER varchar(5)               not null,
  ORDERTYPE       varchar(10)              not null,
  STATUS          varchar(3)               not null,
  ADDDATE         timestamp                not null,
  ADDWHO          varchar(256)             not null,
  COMMENTS        varchar(100) default ' ' not null
);


CREATE TABLE IF NOT EXISTS WMWHSE1.RECEIPT
(
  SERIALKEY        int         NOT NULL PRIMARY KEY,
  EXTERNRECEIPTKEY varchar(50) NOT NULL,
  RECEIPTKEY       varchar(10) NOT NUll,
  TRAILERKEY       varchar(20),
);

CREATE TABLE IF NOT EXISTS wmwhse1.RECEIPTDETAIL
(
  SERIALKEY INT IDENTITY UNIQUE ,
  RECEIPTKEY varchar(10) NOT NULL,
  SKU varchar(50) NOT NULL,
  STATUS varchar(10) NOT NULL,
  QTYEXPECTED decimal(22,5) NOT NULL,
  QTYRECEIVED decimal(22,5) NOT NULL,
  TOLOC varchar(10) NOT NULL,
  STORERKEY varchar(15) NOT NULL,
  LOTTABLE08 nvarchar(50),
  TOLOT varchar(10),
  ADDDATE datetime default '2018-11-22 12:43:54.000',
  EDITDATE datetime default '2018-11-22 12:43:54.000',
  DATERECEIVED datetime default '2018-11-22 12:43:54.000',
  RECEIPTLINENUMBER varchar(5),
  SUSR2 varchar(30),
  SUSR3 varchar(30)
);

CREATE TABLE IF NOT EXISTS WMWHSE1.ORDERS (
  SERIALKEY         int         NOT NULL,
  ORDERKEY          varchar(10) NOT NULL,
  STORERKEY         varchar(15) NOT NULL,
  EXTERNORDERKEY    varchar(50) NOT NULL,
  C_CONTACT1        varchar(30),
  C_CONTACT2        varchar(30),
  C_COMPANY         varchar(45),
  C_ADDRESS1        varchar(45),
  C_ADDRESS2        varchar(45),
  C_ADDRESS3        varchar(45),
  C_ADDRESS4        varchar(45),
  C_ADDRESS5        varchar(45),
  C_ADDRESS6        varchar(45),
  C_CITY            varchar(45),
  C_STATE           varchar(25),
  C_COUNTRY         varchar(30),
  C_PHONE1          varchar(18),
  C_PHONE2          varchar(18),
  TYPE              varchar(10) NOT NULL,
  C_EMAIL1          varchar(55),
  C_EMAIL2          varchar(55),
  SCHEDULEDSHIPDATE timestamp,
  NOTES             varchar(2000),
  NOTES2            varchar(2000),
  CONSTRAINT PK__ORDERS__61857B4BFDED8573 PRIMARY KEY (ORDERKEY)
);
CREATE INDEX IF NOT EXISTS IDX_ORDERS_EXTERNORDERKEY
  ON WMWHSE1.ORDERS (EXTERNORDERKEY);
CREATE UNIQUE INDEX IF NOT EXISTS ORD_172_UNIQUE
  ON WMWHSE1.ORDERS (SERIALKEY);

CREATE TABLE IF NOT EXISTS WMWHSE1.SKU (
  SERIALKEY int NOT NULL,
  SKU varchar(50) NOT NULL,
  PACKKEY varchar(50) NOT NULL DEFAULT 'STD',
  STORERKEY varchar(15),
  SUSR1 varchar(18),
  SUSR2 varchar(18),
  MANUFACTURERSKU varchar(150)
);
CREATE UNIQUE INDEX IF NOT EXISTS SKU_UNIQUE
  ON WMWHSE1.SKU (SKU);

CREATE TABLE IF NOT EXISTS WMWHSE1.TRAILER
(
  SERIALKEY int,
  TRAILERKEY varchar(20) NOT NULL PRIMARY KEY ,
  TRAILERSTATUS varchar(25) NOT NULL,
  EDITDATE datetime,
);

CREATE TABLE IF NOT EXISTS WMWHSE1.TRAILERSTATUS
(
    SERIALKEY int,
    WHSEID varchar(30),
    TRAILERKEY varchar(10) not null,
    TRAILER varchar(25),
    TRAILERTYPE varchar(10),
    TRAILERSTATUS varchar(25) not null,
    ACTIVITYDATE datetime not null,
    ACTIVITYUSER varchar(256),
    ADDDATE datetime,
    ADDWHO varchar(256),
    EDITDATE datetime,
    EDITWHO varchar(256)
);

CREATE TABLE IF NOT EXISTS WMWHSE1.TRANSMITLOG
(
  SERIALKEY INT,
  WHSEID VARCHAR(30),
  TRANSMITLOGKEY VARCHAR(10) NOT NULL
    PRIMARY KEY,
  TABLENAME VARCHAR(30) NOT NULL,
  KEY1 VARCHAR(50) NOT NULL,
  KEY2 VARCHAR(50),
  KEY3 VARCHAR(50),
  KEY4 VARCHAR(50),
  KEY5 VARCHAR(50),
  TRANSMITFLAG VARCHAR(5) NOT NULL,
  TRANSMITFLAG2 VARCHAR(5),
  TRANSMITFLAG3 VARCHAR(5),
  TRANSMITFLAG4 VARCHAR(5),
  TRANSMITFLAG5 VARCHAR(5),
  TRANSMITFLAG6 VARCHAR(5),
  TRANSMITFLAG7 VARCHAR(5),
  TRANSMITFLAG8 VARCHAR(5),
  TRANSMITFLAG9 VARCHAR(5),
  BILLINGTRANSMITFLAG VARCHAR(5),
  LABORTRANSMITFLAG VARCHAR(5),
  TMTRANSMITFLAG VARCHAR(5),
  TRANSMITBATCH VARCHAR(10),
  EVENTSTATUS INT,
  EVENTFAILURECOUNT INT,
  EVENTCATEGORY VARCHAR(1),
  MESSAGE VARCHAR(MAX),
  ADDDATE DATETIME,
  ADDWHO VARCHAR(256),
  EDITDATE DATETIME NOT NULL,
  EDITWHO VARCHAR(256)
);

CREATE INDEX IF NOT EXISTS IDX_TRANSMITLOG_KEY1KEY2
  ON WMWHSE1.TRANSMITLOG (KEY1, KEY2);

CREATE INDEX IF NOT EXISTS IDX_TRANSMITLOG_TNAME
  ON WMWHSE1.TRANSMITLOG (TABLENAME, TRANSMITFLAG);

CREATE INDEX IF NOT EXISTS IDX_XMITFLAG
  ON WMWHSE1.TRANSMITLOG (TRANSMITFLAG);

CREATE INDEX IF NOT EXISTS IDX_TXMITLOG_TAB_XMIT2
  ON WMWHSE1.TRANSMITLOG (TABLENAME, TRANSMITFLAG2);

CREATE TABLE IF NOT EXISTS WMWHSE1.ITRN
(
  SERIALKEY INT,
  WHSEID VARCHAR(30),
  ITRNKEY VARCHAR(10) NOT NULL
    PRIMARY KEY,
  ITRNSYSID INT,
  TRANTYPE VARCHAR(10),
  STORERKEY VARCHAR(15),
  SKU VARCHAR(50) NOT NULL,
  LOT VARCHAR(10),
  FROMLOC VARCHAR(10),
  FROMID VARCHAR(50),
  TOLOC VARCHAR(10),
  TOID VARCHAR(50),
  SOURCEKEY VARCHAR(20),
  SOURCETYPE VARCHAR(30),
  STATUS VARCHAR(10),
  LOTTABLE01 VARCHAR(50),
  LOTTABLE02 VARCHAR(50),
  LOTTABLE03 VARCHAR(50),
  LOTTABLE04 DATETIME,
  LOTTABLE05 DATETIME,
  LOTTABLE06 VARCHAR(50),
  LOTTABLE07 VARCHAR(50),
  LOTTABLE08 VARCHAR(50),
  LOTTABLE09 VARCHAR(50),
  LOTTABLE10 VARCHAR(50),
  CASECNT DECIMAL(22,5),
  INNERPACK DECIMAL(22,5),
  QTY DECIMAL(22,5),
  PALLET DECIMAL(22,5),
  CUBE FLOAT,
  GROSSWGT DECIMAL(22,5),
  NETWGT DECIMAL(22,5),
  OTHERUNIT1 FLOAT,
  OTHERUNIT2 FLOAT,
  PACKKEY VARCHAR(50),
  UOM VARCHAR(10),
  UOMCALC INT,
  UOMQTY DECIMAL(22,5),
  EFFECTIVEDATE DATETIME,
  RECEIPTKEY VARCHAR(10),
  RECEIPTLINENUMBER VARCHAR(10),
  HOLDCODE VARCHAR(10),
  LOTTABLE11 DATETIME,
  LOTTABLE12 DATETIME,
  COUNTSEQUENCE INT,
  TAREWGT DECIMAL(22,5),
  FINALTOLOC VARCHAR(20),
  INTRANSIT VARCHAR(1),
  ADDDATE DATETIME,
  ADDWHO VARCHAR(256),
  EDITDATE DATETIME,
  EDITWHO VARCHAR(256)
);

CREATE INDEX IF NOT EXISTS IDX_ITRN_EFFECTIVEDATE
  ON WMWHSE1.ITRN (EFFECTIVEDATE);

CREATE INDEX IF NOT EXISTS IDX_ITRN_RECKEY
  ON WMWHSE1.ITRN (RECEIPTKEY);

CREATE INDEX IF NOT EXISTS IDX_ITRN_RECLINENO
  ON WMWHSE1.ITRN (RECEIPTLINENUMBER);

CREATE INDEX IF NOT EXISTS IDX_ITRN_EDITDATE
  ON WMWHSE1.ITRN (EDITDATE);

CREATE INDEX IF NOT EXISTS STIL_ITRN
  ON WMWHSE1.ITRN (SOURCEKEY, TRANTYPE, ITRNKEY, LOT);

CREATE INDEX IF NOT EXISTS ITRN_PERF1
  ON WMWHSE1.ITRN (TRANTYPE, SOURCEKEY);

CREATE INDEX IF NOT EXISTS ESSLT_ITRN
  ON WMWHSE1.ITRN (EFFECTIVEDATE, STORERKEY, SKU, LOT, TRANTYPE);


CREATE TABLE IF NOT EXISTS WMWHSE1.HOLDTRN
(
  SERIALKEY INT,
  WHSEID NVARCHAR(30),
  HOLDTRNKEY NVARCHAR(10) NOT NULL
    PRIMARY KEY,
  HOLDTRNGROUP NVARCHAR(10),
  HOLDCODE NVARCHAR(10),
  STORERKEY NVARCHAR(15),
  SKU NVARCHAR(50),
  LOT NVARCHAR(10),
  RANK INT,
  QTY DECIMAL(22,5),
  ORIGIN NVARCHAR(20),
  BEFOREAFTERINDICATOR NVARCHAR(1),
  COUNTSEQUENCE INT,
  COMMENTS NVARCHAR(60),
  LOC NVARCHAR(10),
  ID NVARCHAR(50),
  ADDDATE DATETIME,
  ADDWHO NVARCHAR(256),
  EDITDATE DATETIME,
  EDITWHO NVARCHAR(256)
);

CREATE TABLE IF NOT EXISTS WMWHSE1.BILLOFMATERIAL
(
  SERIALKEY INT NOT NULL,
  WHSEID nvarchar(30),
  STORERKEY nvarchar(15) NOT NULL,
  SKU nvarchar(50) NOT NULL,
  COMPONENTSKU nvarchar(50) NOT NULL,
  SEQUENCE nvarchar(10),
  BOMONLY nvarchar(1),
  QTY decimal(22,5),
  ADDDATE datetime,
  ADDWHO nvarchar(256),
  EDITDATE datetime,
  EDITWHO nvarchar(256),
  NOTES nvarchar(max),
  PRIMARY KEY (STORERKEY, SKU, COMPONENTSKU),
  UNIQUE (STORERKEY, SKU, COMPONENTSKU, SEQUENCE)
);

CREATE TABLE IF NOT EXISTS wmwhse1.LOTXIDDETAIL
(
    SERIALKEY INT NOT NULL,
    WHSEID nvarchar(30),
    LOTXIDKEY nvarchar(10),
    LOTXIDLINENUMBER nvarchar(5),
    WGT decimal(12,6),
    IOTHER1 nvarchar(30),
    IOTHER2 nvarchar(30),
    IOTHER3 nvarchar(30),
    OOTHER1 nvarchar(30),
    OOTHER2 nvarchar(30),
    OOTHER3 nvarchar(30),
    SKU nvarchar(50),
    ETWEIGHT decimal(12,6),
    IOFLAG nvarchar(1),
    LOT nvarchar(10),
    ID nvarchar(50),
    SOURCEKEY nvarchar(10),
    SOURCELINENUMBER nvarchar(10),
    PICKDETAILKEY nvarchar(10),
    VALIDATEFLAG nvarchar(1),
    CAPTUREBY nvarchar(1),
    CASEID nvarchar(50),
    IQTY decimal(22,5),
    OQTY decimal(22,5),
    IOTHER4 nvarchar(30),
    IOTHER5 nvarchar(30),
    OOTHER4 nvarchar(30),
    OOTHER5 nvarchar(30),
    SERIALNUMBERLONG nvarchar(500),
    GROSSWEIGHT decimal(22,5),
    TAREWGT decimal(22,5),
    ITRNKEY nvarchar(10),
    ADDDATE datetime,
    ADDWHO nvarchar(256),
    EDITDATE datetime,
    EDITWHO nvarchar(256),
    primary key (LOTXIDKEY, LOTXIDLINENUMBER)
);

CREATE TABLE IF NOT EXISTS wmwhse1.PICKDETAIL
(
  SERIALKEY                  INT                              NOT NULL,
  WHSEID                     nvarchar(30),
  PICKDETAILKEY              nvarchar(18)                     not null primary key,
  CASEID                     nvarchar(50)   default ''        not null,
  PICKHEADERKEY              nvarchar(18)                     not null,
  ORDERKEY                   nvarchar(10)                     not null,
  ORDERLINENUMBER            nvarchar(5)                      not null,
  LOT                        nvarchar(10)                     not null,
  STORERKEY                  nvarchar(15)                     not null,
  SKU                        nvarchar(50)                     not null,
  ALTSKU                     nvarchar(50)   default ''        not null,
  UOM                        nvarchar(10)   default ''        not null,
  UOMQTY                     decimal(22, 5) default 0         not null,
  QTY                        decimal(22, 5) default 0         not null,
  QTYMOVED                   decimal(22, 5) default 0         not null,
  STATUS                     nvarchar(10)   default '0'       not null,
  DROPID                     nvarchar(50)   default ''        not null,
  LOC                        nvarchar(10)   default 'UNKNOWN' not null,
  ID                         nvarchar(50)   default ''        not null,
  PACKKEY                    nvarchar(50)   default ''        not null,
  UPDATESOURCE               nvarchar(10)   default '0'       not null,
  CARTONGROUP                nvarchar(10),
  CARTONTYPE                 nvarchar(10),
  TOLOC                      nvarchar(10)   default '',
  DOREPLENISH                nvarchar(1)    default 'N',
  REPLENISHZONE              nvarchar(10)   default '',
  DOCARTONIZE                nvarchar(1)    default 'N',
  PICKMETHOD                 nvarchar(1)    default ''        not null,
  WAVEKEY                    nvarchar(10)   default ''        not null,
  EFFECTIVEDATE              datetime,
  FORTE_FLAG                 nvarchar(6)    default 'I'       not null,
  FROMLOC                    nvarchar(10)   default '',
  TRACKINGID                 nvarchar(45),
  FREIGHTCHARGES             float,
  INTERMODALVEHICLE          nvarchar(30),
  LOADID                     nvarchar(20),
  STOP                       int,
  DOOR                       nvarchar(18),
  ROUTE                      nvarchar(18),
  SORTATIONLOCATION          nvarchar(18),
  SORTATIONSTATION           nvarchar(18),
  BATCHCARTONID              nvarchar(18),
  ISCLOSED                   nvarchar(1)    default 'N'       not null,
  QCSTATUS                   nvarchar(10)   default 'P'       not null,
  PDUDF1                     nvarchar(10),
  PDUDF2                     nvarchar(10),
  PDUDF3                     nvarchar(10),
  PICKNOTES                  nvarchar(255),
  RECEIPTKEY                 nvarchar(10),
  CROSSDOCKED                nvarchar(1)    default '0',
  SEQNO                      int            default 99999,
  LABELTYPE                  nvarchar(10),
  COMPANYPREFIX              nvarchar(10),
  SERIALREFERENCE            nvarchar(15),
  ITRNKEY                    nvarchar(10)   default ''        not null,
  ADDDATE                    datetime                         not null,
  ADDWHO                     nvarchar(256)                    not null,
  EDITDATE                   datetime                         not null,
  EDITWHO                    nvarchar(256)                    not null,
  OPTIMIZECOP                nvarchar(1),
  QTYREJECTED                decimal(22, 5) default 0         not null,
  REJECTEDREASON             nvarchar(10),
  STATUSREQUIRED             nvarchar(10)   default 'OK'      not null,
  SELECTEDCARTONTYPE         nvarchar(10),
  SELECTEDCARTONID           nvarchar(50),
  TAREWGT                    decimal(22, 5) default 0         not null,
  NETWGT                     decimal(22, 5) default 0         not null,
  GROSSWGT                   decimal(22, 5) default 0         not null,
  REFID                      nvarchar(50),
  SHIPID                     nvarchar(40)   default ' ',
  EQUIPMENTID                nvarchar(50),
  EQUIPMENTTYPE              nvarchar(10),
  ASSIGNMENTNUMBER           nvarchar(10),
  ALLOCATESTRATEGYKEY        nvarchar(10),
  ALLOCATESTRATEGYLINENUMBER nvarchar(5),
  POSITION                   int,
  PICKCONTPLACEMENT          nvarchar(10),
  DEMANDKEY                  nvarchar(10)
);


CREATE TABLE IF NOT EXISTS wmwhse1.LOC
(
  SERIALKEY                  int not null,
  WHSEID                     nvarchar(30),
  LOC                        nvarchar(10)  default 'UNKNOWN'    not null
    primary key
    check (NOT [LOC] = ''),
  CUBE                       float         default 0            not null,
  LENGTH                     float         default 0            not null,
  WIDTH                      float         default 0            not null,
  HEIGHT                     float         default 0            not null,
  LOCATIONTYPE               nvarchar(10)  default ''           not null,
  LOCATIONFLAG               nvarchar(10)  default 'NONE'       not null,
  LOCATIONHANDLING           nvarchar(10)  default '9'          not null,
  LOCATIONCATEGORY           nvarchar(10)  default 'OTHER'      not null,
  LOGICALLOCATION            nvarchar(18)  default ''           not null,
  CUBICCAPACITY              float         default 0            not null,
  WEIGHTCAPACITY             float         default 0            not null,
  STATUS                     nvarchar(10)  default 'OK'         not null,
  LOSEID                     nvarchar(1)   default '0'          not null,
  FACILITY                   nvarchar(256) default ''           not null,
  SECTION                    nvarchar(10)  default 'FACILITY'   not null,
  ABC                        nvarchar(1)   default 'B'          not null,
  PICKZONE                   nvarchar(10)  default ''           not null,
  PUTAWAYZONE                nvarchar(10)  default ''           not null,
  SECTIONKEY                 nvarchar(10)  default 'FACILITY'   not null,
  PICKMETHOD                 nvarchar(1)   default ''           not null,
  COMMINGLESKU               nvarchar(1)   default '1'          not null,
  COMMINGLELOT               nvarchar(1)   default '1'          not null,
  LOCLEVEL                   int           default 0            not null,
  XCOORD                     int           default 0            not null,
  YCOORD                     int           default 0            not null,
  ZCOORD                     int           default 0            not null,
  OPTLOC                     nvarchar(10),
  StackLimit                 int           default 0            not null,
  FootPrint                  int           default 0            not null,
  ORIENTATION                int,
  INTERLEAVINGSEQUENCE       int           default 0,
  AutoShip                   int           default 0,
  CYCLECLASS                 nvarchar(10),
  LASTCCRELEASEDATE          datetime,
  LASTLOCCOUNTDATE           datetime,
  MAXDOCKASSIGNORDERS        int,
  MAXDOCKASSIGNESTPALLETS    decimal(22, 5),
  BACKFLUSHINDICATOR         nvarchar(1)   default '0',
  PRODSTAGELOC               nvarchar(10),
  LPNSORTALLOCATION          nvarchar(10)  default '1'          not null,
  ALLOCATIONZONE             nvarchar(10)  default '1',
  ADDDATE                    datetime      default now()        not null,
  ADDWHO                     nvarchar(256) default 'ivanov'     not null,
  EDITDATE                   datetime      default now()        not null,
  EDITWHO                    nvarchar(256) default 'ivanov'     not null,
  LOCGROUPID                 int,
  COMMINGLELOTTABLE1         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE2         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE3         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE6         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE7         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE8         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE9         nvarchar(1)   default '1'          not null,
  COMMINGLELOTTABLE10        nvarchar(1)   default '1'          not null,
  CHECKDIGIT                 nvarchar(3),
  SHAREWIDTHFROMLOC          nvarchar(10),
  SHAREWEIGHTCAPACITYFROMLOC nvarchar(10)
);

CREATE TABLE IF NOT EXISTS wmwhse1.LOT
(
	SERIALKEY int,
	WHSEID nvarchar(30),
	LOT nvarchar(10),
	STORERKEY nvarchar(15),
	SKU nvarchar(50),
	CASECNT decimal(22,5),
	INNERPACK decimal(22,5),
	QTY decimal(22,5),
	PALLET decimal(22,5),
	CUBE float,
	GROSSWGT decimal(22,5),
	NETWGT decimal(22,5),
	OTHERUNIT1 float,
	OTHERUNIT2 float,
	QTYPREALLOCATED decimal(22,5),
	QTYALLOCATED decimal(22,5),
	QTYPICKED decimal(22,5),
	QTYONHOLD decimal(22,5),
	STATUS nvarchar(10),
	ARCHIVEQTY decimal(22,5),
	ARCHIVEDATE datetime,
	ADDDATE datetime,
	ADDWHO nvarchar(256),
	EDITDATE datetime,
	EDITWHO nvarchar(256),
	GROSSWGTPREALLOCATED float,
	NETWGTPREALLOCATED float,
	GROSSWGTALLOCATED float,
	NETWGTALLOCATED float,
	GROSSWGTPICKED float,
	NETWGTPICKED float,
	TAREWGT decimal(22,5)
);

CREATE TABLE IF NOT EXISTS wmwhse1.ORDERSTATUSSETUP
(
    SERIALKEY int,
    WHSEID nvarchar(30),
    CODE nvarchar(10),
    CANCEL_FLAG nchar(1)
);

CREATE TABLE IF NOT EXISTS wmwhse1.delivery_service_cutoffs
(
    SERIALKEY             int,
    delivery_service_code nvarchar(15),
    order_creation_cutoff varchar(10),
    picking_cutoff        varchar(10),
    shipping_cutoff       varchar(10),
    warehouse_cutoff      varchar(10),
    edit_who              varchar(255),
    comment               varchar(255)
);

create table if not exists wmwhse1.NSQLCONFIG
(
    SERIALKEY int auto_increment,
    WHSEID nvarchar(30) default 'test',
    CONFIGKEY nvarchar(30) not null primary key,
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
