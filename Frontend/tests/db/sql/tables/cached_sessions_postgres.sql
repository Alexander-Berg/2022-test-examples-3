drop table if exists "CachedSessions";
create table "CachedSessions"
(
    "sessionHash" varchar(255)             not null
        primary key,
    "sessionID"   varchar(255),
    provider      varchar(255),
    "tsFrom"      integer,
    "tsTo"        integer,
    json          json,
    "createdAt"   timestamp with time zone not null,
    "updatedAt"   timestamp with time zone not null
);
