
create schema if not exists dbo;
create table if not exists dbo.E_SSO_USER
(
    fully_qualified_id nvarchar(32) null,
    loginid nvarchar(15)
);

create table if not exists dbo.E_USER
(
    e_user_id                binary(16)    not null
        primary key,
    user_data_id             binary(16)    not null,
    tenant_id                nvarchar(64),
    sso_name                 nvarchar(900),
    active_status_lkp        binary(16)    not null,
    locale_id                nvarchar(30)  not null,
    self_registered_flag     int default 0 not null,
    obsolete_flag            int default 0 not null,
    revision_number          int default 1 not null,
    loginid                  nvarchar(256),
    externuserid             nvarchar(64),
    external_revision_number nvarchar(22),
    session_id               varchar(8)
);

create table if not exists dbo.USER_PREF_INSTANCE
(
    user_pref_instance_id binary(16)    not null
        primary key,
    user_pref_tmpl_name   nvarchar(255) not null,
    user_data_id          binary(16),
    e_portal_user_data_id binary(16),
    user_pref_value       varchar(4000),
    object_name           nvarchar(255),
    revision_number       int default 1 not null,
    obsolete_flag         int default 0 not null
);
