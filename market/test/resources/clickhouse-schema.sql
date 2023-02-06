CREATE SCHEMA IF NOT EXISTS WMS;

CREATE TABLE IF NOT EXISTS WMS.SCANNING_OPERATION
(
    warehouse              varchar(10) not null,
    env                    varchar(100) not null,
    user                   varchar(100) not null,
    operationType          varchar(100)  not null,
    operationDay           datetime  not null,
    operationDateTime      datetime  not null,
    qty                    int not null,
    fromLoc                varchar(100),
    toLoc                  varchar(100),
    fromId                 varchar(100),
    toId                   varchar(100),
    sku                    varchar(100),
    storerKey              varchar(100),
    lot                    varchar(100),
    sourceKey              varchar(100)
);

CREATE TABLE IF NOT EXISTS WMS.MORNING_CUTOFF_TIME
(
    warehouse              varchar(10) not null,
    env                    varchar(100) not null,
    morningCutoffTime      varchar(10)  not null,
    addDate                datetime  not null
);
