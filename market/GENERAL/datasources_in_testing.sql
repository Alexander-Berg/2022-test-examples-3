--liquibase formatted sql

/**
 Состояние магазинов, находящихся на премодерации или на самопроверки или на просто проверке в СКК
*/

CREATE TABLE shops_web.datasources_in_testing (
  --   Идентификатор записи в таблице
  id number(*) PRIMARY KEY NOT NULL,

  -- Идентификатор магазина, для которого сохранено состояние
  datasource_id number(*),

  -- Статус магазина в тестинге (см. TestingStatus.java)
  status int DEFAULT 0 NOT NULL,

  -- Флаг того, что магазин готов к премодерации
  ready number(1),

  -- Флаг того, что магазин допущен к премодерации
  approved number(1),

  -- Флаг того, что магазин находится на премодерации
  in_progress number(1),

  -- Флаг того, что процесс премодерации отменен (в результате ошибок магазина)
  cancelled number(1),

  -- Количество нажатий на кнопку "Начать проверку" в партнерской части интерфейса
  push_ready_count number(*),

  -- Флаг того, что процесс премодерации отменен и не может быть возобновлен нажатием кнопки в партнерском интерфейсе
  fatal_cancelled number(1),

  iter_count number(*),

  updated_at date,

  recommendations clob,

  start_date date,

  testing_type number(*) DEFAULT 0 NOT NULL,

  -- Общее кол-во попыток пройти проверку
  attempt_num int DEFAULT 0 NOT NULL,

  -- Необходимость пройти проверка на качество
  quality_check_required number(1) DEFAULT 0 NOT NULL,

  -- Необходимость пройти проверку на клоновость
  clone_check_required number(1) DEFAULT 0 NOT NULL,

  claim_link number(*) DEFAULT 0 NOT NULL
);

CREATE INDEX i_datasources_in_testing_did ON shops_web.datasources_in_testing (datasource_id);

--changeSet zoom:MBI-MBI-18283_status endDelimiter:;
ALTER TABLE shops_web.datasources_in_testing
  ADD status int DEFAULT 0 NOT NULL;

--changeSet zoom:MBI-MBI-18283_status_comment endDelimiter:;
COMMENT ON COLUMN shops_web.datasources_in_testing.status IS 'Статус магазина в тестинге (см. TestingStatus.java)';


--changeSet zoom:MBI-MBI-18283_attempt_num endDelimiter:;
ALTER TABLE shops_web.datasources_in_testing
  ADD attempt_num int DEFAULT 0 NOT NULL;

--changeSet zoom:MBI-MBI-18283_attempt_num_comment_1 endDelimiter:;
COMMENT ON COLUMN shops_web.datasources_in_testing.attempt_num IS 'Общее кол-во попыток со штрафом и без него';


--changeSet zoom:MBI-MBI-18283_quality_check_required endDelimiter:;
ALTER TABLE shops_web.datasources_in_testing
  ADD quality_check_required number(1) DEFAULT 0 NOT NULL;

--changeSet zoom:MBI-MBI-18283_quality_check_required_comment endDelimiter:;
COMMENT ON COLUMN shops_web.datasources_in_testing.quality_check_required IS 'Необходимость пройти проверку на качество';

--changeSet zoom:MBI-MBI-18283_clone_check_required_1 endDelimiter:;
ALTER TABLE shops_web.datasources_in_testing
  ADD clone_check_required number(1) DEFAULT 0 NOT NULL;

--changeSet zoom:MBI-MBI-18283_clone_check_required_comment endDelimiter:;
COMMENT ON COLUMN shops_web.datasources_in_testing.clone_check_required IS 'Необходимость пройти проверку на клоновость';

--changeSet wadim:MBI-21506_shop_program_add_column
ALTER TABLE shops_web.datasources_in_testing ADD shop_program varchar2(25 char)
  CONSTRAINT dit_shop_program_check CHECK (shop_program in ('CPC', 'CPA', 'SELF_CHECK'));

--changeSet wadim:MBI-21506_shop_program_comment
COMMENT ON COLUMN shops_web.datasources_in_testing.shop_program IS 'Тип программы. CPC, CPC и т.д.';

--changeSet wadim:MBI-21506_shop_program_update
-- записей с типом =2 не должно быть в проде, если есть в тестингах, удалить их руками.
update shops_web.datasources_in_testing set shop_program = 'CPC' where testing_type in (0,1);
update shops_web.datasources_in_testing set shop_program = 'CPA' where testing_type in (3,4);
update shops_web.datasources_in_testing set shop_program = 'SELF_CHECK' where testing_type in (5);

--changeSet wadim:MBI-21506_shop_program_not_null
ALTER TABLE shops_web.datasources_in_testing modify shop_program not null;

--changeSet wadim:MBI-21506_shop_program_datasource_unique
ALTER TABLE shops_web.datasources_in_testing
  add CONSTRAINT uq_dit_program_datasource UNIQUE (datasource_id, shop_program);

--changeSet wadim:MBI-21685_drop_constraint
ALTER TABLE shops_web.datasources_in_testing DROP CONSTRAINT dit_shop_program_check;

--changeSet wadim:MBI-21685_add_constraint
ALTER TABLE shops_web.datasources_in_testing
  ADD CONSTRAINT dit_shop_program_check CHECK (shop_program in ('CPC', 'CPA', 'SELF_CHECK', 'GENERAL'));

--changeset vbauer:MBI-24534-datasources_in_testing-remove-wrong
delete from shops_web.datasources_in_testing where datasource_id in (
  select datasource_id from shops_web.datasources_in_testing dit
    left join shops_web.datasource d on dit.datasource_id = d.id
    where d.id is null
);

--changeset vbauer:MBI-24534-datasources_in_testing-fk-datasource_id
alter table shops_web.datasources_in_testing
  add constraint fk_datasources_in_testing_dsid
    foreign key (datasource_id)
      references shops_web.datasource(id);

--changeSet natalokhina:MBI-59430_drop_constraint
ALTER TABLE shops_web.datasources_in_testing DROP CONSTRAINT dit_shop_program_check;

--changeSet natalokhina:MBI-59430_add_constraint
ALTER TABLE shops_web.datasources_in_testing
  ADD CONSTRAINT dit_shop_program_check CHECK (shop_program in ('CPC', 'CPA', 'SELF_CHECK', 'GENERAL', 'API_DEBUG'));
