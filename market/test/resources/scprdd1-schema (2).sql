create schema if not exists dbo;

create table if not exists dbo.USER_AUDIT
(
    USER_AUDIT_ID    varchar(100)  not null
        primary key,
    USER_DATA_ID     nvarchar(255) not null,
    FULL_NAME        nvarchar(255),
    COMPONENT_NAME   nvarchar(100) not null,
    LOGIN_DATE_TIME  datetime,
    LOGOUT_DATE_TIME datetime,
    IP_ADDRESS       nvarchar(200),
    SESSION_ID       nvarchar(200),
    PAGE_ID          nvarchar(100),
    INSTANCE_NAME    nvarchar(100),
    PORT             nvarchar(10)
)
