create table if not exists YT_DATASOURCE
(
    SERIALKEY int identity,
    NAME      nvarchar(100) not null,
    JDBC      nvarchar(200) not null,
    USERNAME  nvarchar(50)  not null,
    PASSWORD  nvarchar(50)  not null
);

create table if not exists wmwhse1.YT_UPLOAD_TASK
(
    JOB_ID        int                               not null
        primary key,
    RUN_ID        int                               not null,
    DATASOURCE    nvarchar(50)                      not null,
    DB_TABLE      nvarchar(100)                     not null,
    PRIMARY_KEY   nvarchar(100)                     not null,
    APPEND_CURSOR nvarchar(100),
    YT_PATH       nvarchar(512)                     not null,
    ALL_ROWS      int,
    READ_ROWS     int,
    UPLOADED_ROWS int,
    STATUS        nvarchar(20),
    FLAGS         nvarchar(max),
    START_DATE    datetime2,
    FINISH_DATE   datetime2,
    FILTER_DATE   datetime2,
    ADDDATE       datetime2                         not null,
    ADDWHO        nvarchar(256) default 'TEST'      not null,
    EDITDATE      datetime2                         not null,
    EDITWHO       nvarchar(256) default 'TEST'      not null,
    NOTIFIED      bit           default 0           not null
);
