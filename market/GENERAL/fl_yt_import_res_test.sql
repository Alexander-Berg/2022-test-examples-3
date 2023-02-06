--liquibase formatted sql

-- Это все временно
-- В будущем удалится

--changeset batalin:MBI-41630-table
create table shops_web.fl_yt_import_res_test (
    feed_id          number(10, 1) not null,
    indexer_type     number(1)     not null,
    session_name     varchar2(20)  not null,
    cluster_name     varchar2(20)  not null,
    parse_log        clob,
    parse_log_parsed clob,
    constraint pk_fl_yt_import_res_test
        primary key (feed_id, indexer_type, session_name, cluster_name)
)
;

--changeset batalin:MBI-41630-table-comment
comment on table shops_web.fl_yt_import_res_test is 'Сессия обработки фида. Тестовая таблица. Импорт из YT';
comment on column shops_web.fl_yt_import_res_test.feed_id is 'Идентификатор фида';
comment on column shops_web.fl_yt_import_res_test.indexer_type is 'Тип индекса (main/planeshift)';
comment on column shops_web.fl_yt_import_res_test.session_name is 'Идентификатор сессии';
comment on column shops_web.fl_yt_import_res_test.cluster_name is 'Датацентр, в котором была сгенерирована сессия';
comment on column shops_web.fl_yt_import_res_test.parse_log is 'Парс лог';

--changeset batalin:MBI-41650-drop-20
drop table shops_web.fl_yt_import_res_test;
