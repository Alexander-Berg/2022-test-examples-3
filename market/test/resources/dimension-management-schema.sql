create schema if not exists dimension_management;

create table if not exists dimension_management.MEASUREMENTORDER
(
    SERIALKEY int identity primary key,
    SERIALNUMBER nvarchar(30),
    ALTSKU nvarchar(50),
    STATUS nvarchar(15) default 'NEW' not null,
    ASSIGNED nvarchar(256),
    LOC nvarchar(10),
    TYPE nvarchar(15) not null,
    FROM_ID nvarchar(18),
    TO_ID nvarchar(18),
    WEIGHT decimal(18, 3),
    LENGTH decimal(18, 3),
    HEIGHT decimal(18, 3),
    WIDTH decimal(18, 3),
    SKU nvarchar(50) not null,
    STORER nvarchar(15) not null,
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null

);

create table if not exists dimension_management.MEASUREEQUIPMENT
(
    EQUIPMENT_ID nvarchar(64) primary key,
    ENABLED bit not null default 0,
    HOSTNAME nvarchar(256) not null,
    PORT nvarchar(8) not null,
    PATH nvarchar(256),
    LOGIN nvarchar(256) not null,
    PASSWORD nvarchar(256) not null,
    TYPE nvarchar(32) not null,
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null,
    VERSION int default 1 not null
    );

create table if not exists dimension_management.MEASURESTATION
(
    ID int identity primary key,
    LOC nvarchar(10) not null,
    TYPE nvarchar(50) DEFAULT 'STATIONARY_WITH_DEVICE' NOT NULL,
    EQUIPMENT_ID nvarchar(64) unique,
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null,
    CONSTRAINT FK_MEASURESTATION_EQUIPMENT_ID FOREIGN KEY (EQUIPMENT_ID) REFERENCES dimension_management.MEASUREEQUIPMENT(EQUIPMENT_ID)
);

create table if not exists dimension_management.CONTAINERS
(
    ID nvarchar(18) not null primary key,
    STATION_ID int not null,
    STATUS nvarchar(15) not null,
    PARENT_ID nvarchar(18) null,
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null

);

create table if not exists dimension_management.CONFIG_PROPERTIES
(
    ID int identity primary key,
    NAME nvarchar(100) not null unique,
    DESCRIPTION nvarchar(4000),
    VALUE nvarchar(max),
    CATEGORY nvarchar(10),
    COMMENTARY nvarchar(4000),
    ADD_WHO nvarchar(256) default 'test' not null,
    EDIT_WHO nvarchar(256) default 'test' not null,
    ADDDATE datetime default now() not null,
    EDITDATE datetime default now() not null

);

CREATE TABLE IF NOT EXISTS dimension_management.USER_STATION
(
    USER_LOGIN NVARCHAR(256) PRIMARY KEY,
    STATION_ID INT NOT NULL,
    ADDDATE DATETIME DEFAULT now() NOT NULL,
    ADDWHO NVARCHAR(256) DEFAULT 'test' NOT NULL,
    EDITDATE DATETIME DEFAULT now() NOT NULL,
    EDITWHO NVARCHAR(256) DEFAULT 'test' NOT NULL,
    CONSTRAINT FK_USER_STATION_MEASURESTATION FOREIGN KEY (STATION_ID) REFERENCES dimension_management.MEASURESTATION(ID)
);

create table if not exists dimension_management.LOC_TO_STATION
(
    ID int identity primary key,
    LOC nvarchar(10) not null unique,
    STATION_LOC nvarchar(10) not null,
    ADDDATE datetime default now() not null,
    ADDWHO nvarchar(256) default 'test' not null,
    EDITDATE datetime default now() not null,
    EDITWHO nvarchar(256) default 'test' not null,
    CONSTRAINT FK_LOC_TO_STATION FOREIGN KEY (STATION_LOC) REFERENCES dimension_management.MEASURESTATION(LOC)
);
