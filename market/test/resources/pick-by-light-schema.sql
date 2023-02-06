create schema if not exists pick_by_light;

create table if not exists pick_by_light.STATION
(
    id                 int identity primary key,
    station            nvarchar(50)                       not null,
    vendor             nvarchar(50)                       not null,
    host               nvarchar(512)                      not null,
    port               int                                not null,
    address_in_prefix  nvarchar(10)                       not null,
    address_out_prefix nvarchar(10)                       not null,
    add_date           datetime      default now()        not null,
    add_who            nvarchar(256) default 'test_user'  not null,
    edit_date          datetime      default now()        not null,
    edit_who           nvarchar(256) default 'test_user'  not null
);

create unique index if not exists uidx_station on pick_by_light.STATION (station);

create table if not exists pick_by_light.STATION_CELL
(
    id          int identity primary key,
    station     nvarchar(50)                       not null,
    cell        nvarchar(50)                       not null,
    address_in  nvarchar(10)                       not null,
    address_out nvarchar(10)                       not null,
    add_date    datetime      default now()        not null,
    add_who     nvarchar(256) default 'test_user'  not null,
    edit_date   datetime      default now()        not null,
    edit_who    nvarchar(256) default 'test_user'  not null
);

create unique index if not exists uidx_station_cell on pick_by_light.STATION_CELL (station, cell);

create table if not exists pick_by_light.STATION_OPERATION
(
    id          int identity primary key,
    station     nvarchar(50)  not null,
    in_enabled  bit           not null,
    in_mode     nvarchar(50)  not null,
    out_enabled bit           not null,
    out_mode    nvarchar(50)  not null,
    add_date    datetime      not null default now(),
    add_who     nvarchar(256) not null default 'test_user',
    edit_date   datetime      not null default now(),
    edit_who    nvarchar(256) not null default 'test_user'
);

create unique index if not exists uidx_station_operation on pick_by_light.STATION_OPERATION (station);
