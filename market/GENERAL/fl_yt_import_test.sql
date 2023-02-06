--liquibase formatted sql

-- Это все временно
-- В будущем удалится

--changeset batalin:MBI-41648-table-2
create table shops_web.fl_yt_import_test (
    meta_id     number primary key,
    is_prepared number(1) default 0 not null
);

--changeset batalin:MBI-41648-table-comment-2
comment on table shops_web.fl_yt_import_test is 'Статус поколений для импорта ошибок из yt. Скоро удалится';
comment on column shops_web.fl_yt_import_test.meta_id is 'id поколения';
comment on column shops_web.fl_yt_import_test.is_prepared is 'Подготавливали ли process_log';

--changeset batalin:MBI-41650-drop10
drop table shops_web.fl_yt_import_test;
