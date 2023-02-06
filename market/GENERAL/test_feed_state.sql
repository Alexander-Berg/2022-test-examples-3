--liquibase formatted sql

--changeset berest:MBI-17256.3 endDelimiter:;
CREATE TABLE shops_web.test_feed_state(
  fid INTEGER,
  last_gid INTEGER NOT NULL,
  LAST_RET_CODE INTEGER NOT NULL,
  LAST_RET_CODE_DUR INTEGER NOT NULL,
  RET_CODE_PREV1 INTEGER,
  RET_CODE_PREV2 INTEGER
) TABLESPACE shops_web_dts;

--changeset berest:MBI-17256.4 endDelimiter:;
CREATE INDEX shops_web.i_test_feed_state_fid ON shops_web.test_feed_state(fid) TABLESPACE shops_web_its;

--changeset berest:MBI-17256.5 endDelimiter:;
CREATE INDEX shops_web.i_test_feed_state_last_gid ON shops_web.test_feed_state(last_gid) TABLESPACE shops_web_its;

--changeset saferif:MBI-17468_add_missing_columns_test endDelimiter:;
alter table shops_web.test_feed_state
add (
  download_time DATE default null,
  noffers NUMBER(20,0) default null,
  cpa_offers number(20,0) default null,
  cpa_real_offers number(20,0) default null
);

--changeset saferif:MBI-17468_migrate_data_test endDelimiter:;
merge into shops_web.test_feed_state fs
using (
  select fl.fid fid, g.id gid, fl.download_time, fl.noffers, fl.cpa_offers, fl.cpa_real_offers from shops_web.test_generation_and_delta g, shops_web.test_feed_log_and_delta fl
  where g.id = (select id from shops_web.v_last_test_generation)
  and fl.gid = g.id
) dt
on (fs.fid = dt.fid and fs.last_gid = dt.gid)
when matched then update set fs.download_time = dt.download_time,
  fs.noffers = dt.noffers,
  fs.cpa_offers = dt.cpa_offers,
  fs.cpa_real_offers = dt.cpa_real_offers
;


--changeset sviperll:MBI-24232-test-1 endDelimiter:;
alter table shops_web.test_feed_state
add (
  last_delta_gid INTEGER DEFAULT NULL,
  last_success_delta_gid INTEGER DEFAULT NULL,
  last_success_gid INTEGER DEFAULT NULL
);

--changeset sviperll:MBI-24232-test-2 endDelimiter:;
COMMENT ON COLUMN shops_web.test_feed_state.last_delta_gid IS
    'Последнее не фатальное поколение, полное или нет';

--changeset sviperll:MBI-24232-test-3 endDelimiter:;
COMMENT ON COLUMN shops_web.test_feed_state.last_success_delta_gid IS
    'Последнее успешное (возможно с предупреждениями, но без ошибок) поколение, полное или нет';

--changeset sviperll:MBI-24232-test-4 endDelimiter:;
COMMENT ON COLUMN shops_web.test_feed_state.last_success_gid IS
    'Последнее успешное (возможно с предупреждениями, но без ошибок) полное поколение';


--changeset fbokovikov:MBI-27474-test-feed-state-fields
alter table shops_web.test_feed_state
add (
  red_offers INTEGER,
  red_real_offers INTEGER
);

--changeset fbokovikov:MBI-27474-test-feed-state-fields-comments
COMMENT ON COLUMN shops_web.test_feed_state.red_offers IS 'Количество предложений на Красном Маркете (SBX и REAL)';
COMMENT ON COLUMN shops_web.test_feed_state.red_real_offers IS 'Количество предложений на Красном Маркете (REAL)';

--changeset gaklimov:MBI-39763-test_feed_state-total_offers
alter table shops_web.test_feed_state
    add total_offers number(20, 0);

--changeset gaklimov:MBI-39763-test_feed_state-total_offers-comment_1
comment on column shops_web.test_feed_state.total_offers is
    'Общее количество оферов в фиде, включая отброшенные парсером';

--changeset nastik:MBI-40906-add-feed-process-type-for-datacamp
alter table shops_web.test_feed_state add feed_processing_type varchar2(10 char);
comment on column shops_web.test_feed_state.feed_processing_type is 'Тип обработки фида партера, схема работы';

--changeset batalin:MBI-46662
drop table shops_web.test_feed_state;
