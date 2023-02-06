SET MODE MSSQLServer;

-- copy-paste from: https://bb.yandex-team.ru/projects/MARKET-ERP/repos/sql/browse/INT/TABLES/CREATE/MBOImpCategory.sql
-- drop table MBOImpCategory;
create table MBOImpCategory
(
    HYPER_ID         int             not null,
    NAME             nvarchar(255),
    PARENT_ID        int             not null,
    DELETED          bit             not null,
    PUBLISHED        bit             not null,
    SESSION_ID       bigint          not null,
    UNIQUE_NAME      nvarchar(255),
    NOTES            nvarchar(1000),

    MOD_TS           datetime        not null,
    EXPORT_TS        datetime        not null,
    IMPORT_TS        datetime,
    ID               bigint identity not null,
    IMPORT_STATUS    int             not null,
    NEED_CERTIFICATE bit default 0   not null
);

create index MboImpCatSessionIdIdx on MBOImpCategory (SESSION_ID);
create index MboImpCatStatusIdx on MBOImpCategory (IMPORT_STATUS);

-- copy-paste from: https://bb.yandex-team.ru/projects/MARKET-ERP/repos/sql/browse/INT/TABLES/CREATE/MBOImpSKU.sql
-- drop table MBOImpSKU;
create table MBOImpSKU
(
    MSKU          bigint          not null,
    NAME          nvarchar(500),
    DELETED       bit             not null,
    PUBLISHED     bit             not null,
    SESSION_ID    bigint          not null,
    CATEGORY_ID   int             not null,
    VENDOR_ID     int,
    VENDOR_NAME   nvarchar(255),
    VENDOR_CODE   nvarchar(500),
    PICTURE_URL   nvarchar(1000),
    GROSS_WIDTH   numeric(32, 16),
    GROSS_LENGTH  numeric(32, 16),
    GROSS_DEPTH   numeric(32, 16),
    GROSS_WEIGHT  numeric(32, 16),

    MOD_TS        datetime        not null,
    EXPORT_TS     datetime        not null,
    IMPORT_TS     datetime,
    ID            BIGINT identity not null,
    IMPORT_STATUS int             not null,

    BARCODES      nvarchar(500),
    IS_PSKU       bit default 0
);

create index MboImpSkuSessionIdIdx on MBOImpSKU (SESSION_ID);

