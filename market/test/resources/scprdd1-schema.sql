create schema if not exists dbo;

-- binary(16) types replaced with nvarchar(64) for valid testing

create table if not exists dbo.e_user
(
    e_user_id                nvarchar(64)  not null
        primary key,
    user_data_id             nvarchar(64)  not null,
    tenant_id                nvarchar(64),
    sso_name                 nvarchar(900),
    active_status_lkp        nvarchar(64)  not null,
    locale_id                nvarchar(30)  not null,
    self_registered_flag     int default 0 not null,
    obsolete_flag            int default 0 not null,
    revision_number          int default 1 not null,
    loginid                  nvarchar(256),
    externuserid             nvarchar(64),
    external_revision_number nvarchar(22),
    session_id               varchar(8)
);

create table if not exists dbo.e_sso_user
(
    e_sso_user_id      nvarchar(64)  not null
        primary key,
    sso_user_name      nvarchar(100) not null,
    sso_hash_password  nvarchar(255) not null,
    fully_qualified_id nvarchar(255) not null,
    loginid            nvarchar(256),
    externuserid       nvarchar(64),
    tenant_id          nvarchar(64)
);

create table if not exists dbo.e_sso_role
(
    e_sso_role_id nvarchar(64)  not null
        primary key,
    sso_role_name nvarchar(100) not null,
    tenant_id     nvarchar(64)
);

create table if not exists dbo.user_data
(
    user_data_id             nvarchar(64)  not null
        primary key,
    full_name                nvarchar(255) not null,
    email_address            nvarchar(255),
    user_type_lkp            nvarchar(64)  not null,
    revision_number          int default 1 not null,
    obsolete_flag            int default 0 not null,
    loginid                  nvarchar(256),
    externuserid             nvarchar(64),
    external_revision_number nvarchar(22),
    tenant_id                nvarchar(64)
);

create table if not exists dbo.e_sso_user_role
(
    e_sso_user_role_id nvarchar(64) not null
        primary key,
    sso_role_id        nvarchar(64) not null,
    sso_user_id        nvarchar(64) not null,
    externuserid       nvarchar(64)
);

create table if not exists dbo.user_pref_instance
(
    user_pref_instance_id nvarchar(64)    not null
        primary key,
    user_pref_tmpl_name   nvarchar(255) not null,
    user_data_id          nvarchar(64),
    e_portal_user_data_id nvarchar(64),
    user_pref_value       varchar(4000),
    object_name           nvarchar(255),
    revision_number       int default 1 not null,
    obsolete_flag         int default 0 not null
);


create table if not exists dbo.SKILLS
(
    CODE        nvarchar(100)                not null
    primary key,
    PROCESS     nvarchar(512)                not null,
    ADDDATE     datetime      default now()  not null,
    ADDWHO      nvarchar(256) default 'test' not null,
    EDITDATE    datetime      default now()  not null,
    EDITWHO     nvarchar(256) default 'test' not null,
    IS_DEFAULT  bit           default 0      not null
);

create table if not exists dbo.EMPLOYEE_SKILLS
(
    USERID    nvarchar(100)                not null,
    SKILLCODE nvarchar(100)                not null,
    PRIORITY  int                          not null,
    ADDDATE   datetime      default now()  not null,
    ADDWHO    nvarchar(256) default 'test' not null,
    EDITDATE  datetime      default now()  not null,
    EDITWHO   nvarchar(256) default 'test' not null,
    constraint PK__EMPLOYEE_SKILLS_PK primary key (USERID, SKILLCODE),
    FOREIGN KEY (SKILLCODE) REFERENCES dbo.SKILLS (CODE)
);

CREATE ALIAS IF NOT EXISTS BINARY AS
$$
    @CODE
    String binary(int length) {
        return "binary" + length;
    }
$$;
CREATE ALIAS IF NOT EXISTS NVARCHAR AS
$$
    @CODE
    String nvarchar(int length) {
        return "nvarchar" + length;
    }
$$;
CREATE ALIAS IF NOT EXISTS IIF AS
$$
    @CODE
    String iif(boolean predicate, String ifTrue, String ifFalse) {
        return predicate ? ifTrue : ifFalse;
    }
$$;
CREATE ALIAS IF NOT EXISTS CONVERT AS
$$
    @CODE
    String convert(String dstType, String value, int conversionType) {
        return value;
    }
$$;
