set mode MSSQLServer;

create table PurchReplIN (
    msku bigint not null,
    warehouse_id int not null,
    rs_id nvarchar(20) not null,
    purch_qty int not null,
    order_date datetime not null,
    xdoc_date datetime null,
    mod_ts datetime not null,
    export_ts datetime not null,
    author_id nvarchar(20) not null,
    id_line bigint not null,
    import_ts datetime NULL,
    id bigint identity(1,1) not null,
    import_status int default(0),
    group_id bigint null default(0),
    delivery_type int not null,
    warehouse_id_from int NULL,
    ssku varchar(225) NULL,
    purch_type nvarchar(50) NULL,
    auto_processing int null default(0),
    purch_price money,
    mono_xdoc int null default(0),
    auto_processing_edi int null default(0),
    consolidatedsupply nvarchar(40),
    timetosend datetime null
);

create table PURCHTABLEREPLENISHMENT (
    id_line bigint not null,
    transferstatus varchar(20) not null,
    purchid varchar(20),
    comment text
);

create table PURCHTABLESTATUSVIEW (
    PURCHID nvarchar(50) not null,
    GROUP_ID bigint not null,
    STATUS_ID bigint not null,
    STATUS nvarchar(255) not null,
    DESCRIPTION text not null,
    CREATED_TS datetime not null
);

create table PurchLineAfterConfirmationView (
    PURCH_ID nvarchar(50) not null,
    GROUP_ID bigint not null,
    FF_ID bigint not null,
    SSKU nvarchar(131) not null,
    NEW_PURCH_QTY numeric(32, 16) not null,
    CREATED_DATETIME datetime
);
