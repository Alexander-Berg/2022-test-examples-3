package ru.yandex.market.mbo.integration.test.tt;

import org.springframework.jdbc.core.JdbcOperations;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class TtTaskInitializer {
    //TODO этот класс надо в будущем заменить на инициализацию через ликвибэйз
    public static void create(JdbcOperations siteCatalogPgJdbcTemplate) {
        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.tt_task (\n" +
            "    id                  bigint not null\n" +
            "        constraint tt_task_pk primary key,\n" +
            "    task_list_id        bigint not null,\n" +
            "    content_id          bigint not null,\n" +
            "    last_status         bigint default 0 not null,\n" +
            "    last_status_time    timestamp with time zone default '2000-01-01 00:00:00+03'::timestamp with time " +
            "zone not null,\n" +
            "    last_comment        varchar(4000),\n" +
            "    last_comment_author bigint,\n" +
            "    last_comment_id     bigint,\n" +
            "    created_time        timestamp with time zone default CURRENT_TIMESTAMP\n" +
            ")");
        siteCatalogPgJdbcTemplate.update("TRUNCATE site_catalog.tt_task");

        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.tt_task_list (\n" +
            "    id                  bigint not null\n" +
            "        constraint tt_task_list_pk primary key,\n" +
            "    type                bigint not null,\n" +
            "    category_id         bigint not null,\n" +
            "    priority            bigint not null,\n" +
            "    period              bigint default 0 not null,\n" +
            "    last_status         bigint default 0 not null,\n" +
            "    last_status_time    timestamp with time zone default '2000-01-01 00:00:00+03'::timestamp with time " +
            "zone not null,\n" +
            "    last_user           bigint                   default 0,\n" +
            "    last_opened         timestamp with time zone,\n" +
            "    last_comment        varchar(4000),\n" +
            "    last_comment_author bigint,\n" +
            "    last_comment_id     bigint,\n" +
            "    created_time        timestamp with time zone default CURRENT_TIMESTAMP\n" +
            ")");
        siteCatalogPgJdbcTemplate.update("TRUNCATE site_catalog.tt_task_list");

        siteCatalogPgJdbcTemplate.update("create or replace view site_catalog.v_tt_task_list as\n" +
            "select\n" +
            "    tt.id task_list_id,\n" +
            "    tt.type task_list_type,\n" +
            "    tt.category_id,\n" +
            "    tt.last_status task_list_status,\n" +
            "    tt.priority priority,\n" +
            "    tt.last_user owner_id,\n" +
            "    tt.period,\n" +
            "    (coalesce(tt.last_opened, tt.last_status_time) + make_interval(days := tt.period::int)) as " +
            "deadline\n" +
            "FROM site_catalog.tt_task_list tt");

        siteCatalogPgJdbcTemplate.update("create or replace view site_catalog.v_tt_task as\n" +
            "SELECT\n" +
            "    tt_task.id,\n" +
            "    tt_task.task_list_id,\n" +
            "    tt_task.content_id,\n" +
            "    TT_TASK.LAST_STATUS status,\n" +
            "    tt_task.LAST_STATUS_TIME status_time\n" +
            "FROM site_catalog.tt_task");

        siteCatalogPgJdbcTemplate.update("CREATE SEQUENCE IF NOT EXISTS site_catalog.tt_id_seq");
        createVTtFullTask(siteCatalogPgJdbcTemplate);
        createVTtFullTaskList(siteCatalogPgJdbcTemplate);
    }

    private static void createVTtFullTask(JdbcOperations siteCatalogPgJdbcTemplate) {
        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.TT_STATUS(\n" +
            "  ID    bigint not null\n" +
            "    constraint TT_STATUS_PK\n" +
            "      primary key,\n" +
            "  DESCR varchar(128) default ' '\n" +
            ")");
        siteCatalogPgJdbcTemplate.update("insert into SITE_CATALOG.TT_STATUS (ID, DESCR)\n" +
            "values  (10, 'Возвращено на доработку'),\n" +
            "        (15, 'Задача отменена'),\n" +
            "        (23, 'Проверка задания'),\n" +
            "        (5, 'Задача завершена'),\n" +
            "        (6, 'Не ошибка'),\n" +
            "        (7, 'Проверено, не ошибка'),\n" +
            "        (8, 'Нет данных'),\n" +
            "        (9, 'Задание завершено'),\n" +
            "        (21, 'На обходе роботом'),\n" +
            "        (22, 'Ошибка обхода роботом'),\n" +
            "        (0, 'Initial'),\n" +
            "        (1, 'Задание открыто'),\n" +
            "        (13, 'Возвращено на доработку'),\n" +
            "        (19, 'Проверка результатов обхода'),\n" +
            "        (11, 'Принято (проверено)'),\n" +
            "        (12, 'Принято (без проверки)'),\n" +
            "        (20, 'На обходе роботом'),\n" +
            "        (14, 'Задание закрыто'),\n" +
            "        (17, 'Принято (проверено)'),\n" +
            "        (18, 'Принято (без проверки)'),\n" +
            "        (2, 'Задача открыта'),\n" +
            "        (3, 'Задание выполняется'),\n" +
            "        (4, 'Задача выполняется'),\n" +
            "        (16, 'Задание удалено'),\n" +
            "        (24, 'Проверено (нет данных)'),\n" +
            "        (25, 'Задача отклонена') ON CONFLICT DO NOTHING");

        siteCatalogPgJdbcTemplate.update("create or replace view site_catalog.v_tt_full_task as\n" +
            "select\n" +
            "    tt.last_user owner_id,\n" +
            "    tt.type task_list_type,\n" +
            "    tt.category_id,\n" +
            "    tt_task.id,\n" +
            "    tt_task.task_list_id,\n" +
            "    tt_task.content_id,\n" +
            "    tt_task.last_status status,\n" +
            "    tt_status.descr status_name,\n" +
            "    tt_task.last_status_time status_time,\n" +
            "    tt_task.last_comment message,\n" +
            "    tt_task.last_comment_id message_id,\n" +
            "    tt_task.last_comment_author message_author\n" +
            "from tt_task\n" +
            "         inner join site_catalog.tt_status on tt_task.last_status = tt_status.id\n" +
            "         inner join site_catalog.tt_task_list tt on tt.id = tt_task.task_list_id");
    }

    private static void createVTtFullTaskList(JdbcOperations siteCatalogPgJdbcTemplate) {
        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.NG_GURU_CATEGORY(\n" +
            "  id   bigint  not null " +
            "    constraint NG_GURU_CATEGORY_PK\n" +
            "      primary key,\n" +
            "  name text    not null)");

        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.v_leaf_visual_categories (\n" +
            "    category_id     bigint  not null,\n" +
            "    full_name       text    not null)");

        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.tt_type (\n" +
            "    id     bigint  not null" +
                "   constraint tt_type_pk" +
                "                  primary key,\n" +
            "    descr  text    not null)");
        siteCatalogPgJdbcTemplate.update("insert into site_catalog.tt_type (id, descr)\n" +
            "values  (22, 'Формализация гурулайт категорий'),\n" +
            "        (32, 'Проверка кластера'),\n" +
            "        (24, 'Проверка матчинга баркодов'),\n" +
            "        (25, 'Обучение обхода сайта с баркодами'),\n" +
            "        (26, 'Проверка ссылок для моделей'),\n" +
            "        (27, 'Обучение обхода магазина'),\n" +
            "        (28, 'Обучение обхода сайта с обзорами'),\n" +
            "        (29, 'Обучение обхода сайта с ссылками для моделей'),\n" +
            "        (30, 'Обучение обхода сайта с лайт-карточками'),\n" +
            "        (23, 'Обработка лога от производителя'),\n" +
            "        (0, 'Заполнение карточек моделей'),\n" +
            "        (1, 'Обработка лога'),\n" +
            "        (17, 'Проверка карточек модификаций'),\n" +
            "        (18, 'Проверка лога'),\n" +
            "        (19, 'Классификация ссылки (для составления черного списка)'),\n" +
            "        (2, 'Исправление ошибок в моделях'),\n" +
            "        (3, 'Проверка задания по заполнению карточек моделей'),\n" +
            "        (4, 'Исправление ошибок в модификациях'),\n" +
            "        (20, 'Заполнение карточек модификаций'),\n" +
            "        (21, 'Проверка задания по заполнению карточек модификаций'),\n" +
            "        (31, 'Проверка пар офферов'),\n" +
            "        (33, 'Проверка Турецкой микро-карточки'),\n" +
            "        (34, 'Проверка матчинга турецких предложений'),\n" +
            "        (40, 'Обучение сбора страниц моделей'),\n" +
            "        (43, 'Проверка автосгенеренных карточек моделей'),\n" +
            "        (44, 'Пост-проверка автосгенеренных карточек моделей'),\n" +
            "        (36, 'Турция. Проверка значений параметров моделек'),\n" +
            "        (37, 'Проверка тайтлов'),\n" +
            "        (38, 'Обучение тайтлмейкера'),\n" +
            "        (39, 'Проверка пар офферов (оценка)'),\n" +
            "        (41, 'Проверка ссылок на обзоры'),\n" +
            "        (42, 'Проверка ссылок на видеообзоры'),\n" +
            "        (45, 'Проверка гипотез кластеризатора'),\n" +
            "        (35, 'Проверка вендоров'),\n" +
            "        (46, 'Проверка карточек моделей от вендора') ON CONFLICT DO NOTHING");

        siteCatalogPgJdbcTemplate.update("create table if not exists site_catalog.mbo_user(\n" +
            "    id bigint not null,\n" +
            "    name text,\n" +
            "    login varchar(64),\n" +
            "    email varchar(254),\n" +
            "    creation_time timestamp with time zone,\n" +
            "    staff_login varchar(128)\n" +
            ")");

        siteCatalogPgJdbcTemplate.update("create or replace view site_catalog.v_tt_full_task_list as (\n" +
            "select tt.id as task_list_id,\n" +
            "       tt.type as task_list_type,\n" +
            "       tt.category_id,\n" +
            "       tt.period,\n" +
            "       coalesce(\n" +
            "               (select cat.name from site_catalog.ng_guru_category cat where tt.category_id = cat.id " +
            "limit 1),\n" +
            "               (select full_name from site_catalog.v_leaf_visual_categories where category_id = tt" +
            ".category_id limit 1)\n" +
            "           ) category_name,\n" +
            "       (select tp.descr from site_catalog.tt_type tp where tt.type = tp.id) task_list_type_descr,\n" +
            "       tt.last_status task_list_status,\n" +
            "       tt.last_opened opened_time,\n" +
            "       tt.last_status_time status_time,\n" +
            "       (coalesce(tt.last_opened, tt.last_status_time) + make_interval(days := tt.period::int)) AS " +
            "deadline,\n" +
            "       (select tts.descr from site_catalog.tt_status tts where tt.last_status = tts.id) " +
            "task_list_status_descr,\n" +
            "       tt.priority priority,\n" +
            "       tt.last_user owner_id,\n" +
            "       (select u.name from site_catalog.mbo_user u where tt.last_user = u.id) owner_name,\n" +
            "       (select count(id) from site_catalog.tt_task where task_list_id = tt.id) total_tasks,\n" +
            "       (select count(id) from site_catalog.tt_task where last_status = 4 and task_list_id = tt.id) " +
            "undone_tasks,\n" +
            "       tt.last_comment message,\n" +
            "       tt.last_comment_id message_id,\n" +
            "       tt.last_comment_author message_author\n" +
            "from site_catalog.tt_task_list tt)");
    }
}
