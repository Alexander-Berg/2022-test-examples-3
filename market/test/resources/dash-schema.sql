CREATE SCHEMA IF NOT EXISTS dbo;

CREATE TABLE IF NOT EXISTS dbo.vperfomance_history4
(
    whs                    varchar(10) not null,
    operation_group        varchar(100) not null,
    "Сотрудник"            varchar(100) not null,
    "Staff/OutStaff"       varchar(20)  not null,
    "Новый сотрудник"      varchar(50)  not null,
    "Администратор"        varchar(20)  not null,
    "Ячейка"               varchar(50),
    d1                     datetime,
    "Количество"           int,
    "Время"                int,
    ye                     float,
    ye_abs                 float,
    "Шт/час"               float,
    "Час"                  varchar(20)  not null,
    "Смена"                varchar(30)  not null,
    "YЕ за смену"          varchar(7),
    "Статус эффективности" varchar(30),
    "Опер.день"            date         not null,
    primary key ("Опер.день", "Смена", operation_group, "Сотрудник", "Час")
);

CREATE TABLE IF NOT EXISTS dbo.v1hour_user_ye
(
    d1                   datetime2    not null,
    user_name            varchar(100) not null,
    one_ye               int          not null,
    qty                  int,
    mins                 DECIMAL(10, 4),
    "Производительность" DECIMAL(10, 4),
    "Эффективность"      DECIMAL(10, 4)
);

CREATE TABLE IF NOT EXISTS dbo.vperfomance_DICT
(
    "ABS одного уе"      NUMERIC (13),
    operation_nmb        VARCHAR(2) PRIMARY KEY,
    "Операция"           VARCHAR(1000),
    "Участок"            VARCHAR(16),
    "Данные для расчета" VARCHAR(16),
    "Единица измерения"  VARCHAR(4),
    "Расчет по часам"    INTEGER,
    "УЕ штуки/НЗН"       FLOAT,
    "УЕ товаров с СГ"    FLOAT,
    "УЕ посылки"         FLOAT,
    "УЕ СКУ"             FLOAT,
    "УЕ Ячейки"          FLOAT,
    "БО staff"           FLOAT,
    WHS                  VARCHAR(10)
    );
