SET MODE MSSQLServer;

create table RSSKU_TO_MSKU (
  RS_ID         nvarchar(20),
  SSKU          nvarchar(80),
  RSSKU         nvarchar(80),
  MSKU          bigint not null,
  TITLE         nvarchar(1000),
  BARCODES      nvarchar(500),

  DELETED       bit not null,

  MOD_TS        datetime not null,
  EXPORT_TS     datetime not null,
  IMPORT_TS     datetime,
  ID            bigint identity not null,
  IMPORT_STATUS int not null,
  SUPPLIER_ID   bigint not null,
  STATUS        nvarchar(10),
  CATEGORY_ID   int,
  VENDOR_ID     int,
  VENDOR_NAME   nvarchar (255)
);

create table mbossku_supply (
  RS_ID             NVARCHAR(20) not null,
  SSKU              NVARCHAR(80) not null,
  SHIPMENT_QUANTUM  int,
  MIN_SHIPMENT      int,
  DELIVERY_TIME     int,
  WAREHOUSE_ID      int,
  CALENDAR_ID       nvarchar(40),

  MOD_TS            datetime not null,
  EXPORT_TS         datetime not null,
  IMPORT_TS         datetime,
  ID                bigint identity not null,
  IMPORT_STATUS     int not null default 0,
  QTY_IN_PACK       int
);

create index mbossku_supply_status_idx on mbossku_supply (IMPORT_STATUS);

create table MBOMapCheckOUT (
  RS_ID      nvarchar(20),
  SSKU       nvarchar(80),
  RSSKU      nvarchar(80),
  MSKU       bigint   not null,
  NAME       nvarchar(1000),
  RS_NAME    nvarchar(1000),
  RSSKU_NAME nvarchar(1000),
  EXPORT_TS  datetime not null,
  ISDELETEDINMBO int,
  SUPPLIER_ID  bigint
);

create table MBOCertificateIN
(
  CERT_ID       bigint        not null,
  TYPE          bigint        not null,
  REG_NUMBER    nvarchar(100) not null,
  START_DATE    date          not null,
  END_DATE      date          not null,
  PICTURE_URL   nvarchar(1000),
  DELETED       bit default 0 not null,
  ID            bigint identity,
  MOD_TS        datetime      not null,
  EXPORT_TS     datetime      not null,
  IMPORT_TS     datetime,
  IMPORT_STATUS int default 0 not null
);

CREATE TABLE MBOSKUCertificateIN
(
  CERT_ID       bigint        not null,
  SSKU          nvarchar(80)  not null,
  DELETED       bit default 0 not null,
  ID            bigint identity,
  MOD_TS        datetime      not null,
  EXPORT_TS     datetime      not null,
  IMPORT_TS     datetime,
  IMPORT_STATUS int default 0 not null
);

CREATE TABLE MDMSSKUMasterDataIN
(
  ID            bigint        identity,
  SSKU          nvarchar(80)  not null,
  SSKU_DATA     nvarchar(max)  not null,
  EXPORT_TS     datetime      not null,
  IMPORT_TS     datetime,
  IMPORT_STATUS int default 0 not null
);

CREATE TABLE MBOSSKUMasterDataIN
(
    ID            int identity,
    SSKU          nvarchar(131) not null,
    SSKU_DATA     nvarchar(max) not null,
    IMPORT_TS     datetime,
    IMPORT_STATUS int           not null,
    EXPORT_TS     datetime      not null,
    RETRY_COUNT   int
);
