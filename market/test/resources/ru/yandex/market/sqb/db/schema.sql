
-- HSQLDB Management
-- http://hsqldb.org/doc/guide/management-chapt.html

set database sql ignorecase true;
set database sql syntax ora true;

-- Создать схемы

create schema shops_web;


-- Создать таблицу "shops_web.datafeed"

create table shops_web.datafeed (
    is_enabled number(1) default 0 not null,
    url varchar2(1024) not null,
    login varchar2(50),
    password varchar2(50),
    datasource_id number(10) not null,
    id number(10),
    upload_id number(10,1)
);

create unique index c_datafeed_id on shops_web.datafeed (id);
create index idx_datafeed_datasource_id on shops_web.datafeed (datasource_id);


-- Создать таблицу "shops_web.datafeed"

create table shops_web.datasource (
    name varchar2(50),
    id number(10) primary key not null,
    created_at date,
    comments varchar2(510),
    manager_id number not null
);
create index idx_datasource_name on shops_web.datasource (name);
