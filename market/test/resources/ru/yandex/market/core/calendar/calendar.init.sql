CREATE SCHEMA shops_web;

CREATE TABLE shops_web.calendar_owner_types (
  id int PRIMARY KEY,
  name varchar2(256) NOT NULL UNIQUE
);

INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (0, 'Биллинг');
INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (1, 'Регион');
INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (2, 'Магазин');
INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (3, 'Служба доставки');
INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (4, 'Мультиключ');
INSERT INTO shops_web.calendar_owner_types (id, name) VALUES (5, 'Точка продаж');

CREATE TABLE shops_web.calendar_types (
  id int,
  owner_type_id int NOT NULL,
  name varchar2(256) NOT NULL,
  CONSTRAINT pk_calendar_types PRIMARY KEY (id),
  CONSTRAINT fk_calendar_types_owner
  FOREIGN KEY (owner_type_id)
  REFERENCES shops_web.calendar_owner_types (id),
  CONSTRAINT uk_calendar_types_name UNIQUE (name)
);
CREATE INDEX shops_web.i_calendar_type_owner_type_id ON shops_web.calendar_types (owner_type_id);

INSERT INTO shops_web.calendar_types (id, owner_type_id, name) VALUES (1, 1, 'Календарь региональных праздников');
INSERT INTO shops_web.calendar_types (id, owner_type_id, name)
VALUES (2, 2, 'Календарь рабочих/нерабочих дней службы доставки магазина');
INSERT INTO shops_web.calendar_types (id, owner_type_id, name)
VALUES (3, 2, 'Дефолтный календарь рабочих/нерабочих дней служб доставки магазинов региона');
INSERT INTO shops_web.calendar_types (id, owner_type_id, name)
VALUES (4, 0, 'Дефолтный календарь служб доставки магазинов');

CREATE SEQUENCE shops_web.calendars_seq;
CREATE TABLE shops_web.calendars (
  id int DEFAULT shops_web.calendars_seq.nextval PRIMARY KEY,
  type_id int NOT NULL REFERENCES shops_web.calendar_types (id),
  owner_id int NOT NULL,
  name varchar2(256) NOT NULL
);
CREATE INDEX shops_web.i_calendars_type_id ON shops_web.calendars (type_id);
CREATE UNIQUE INDEX shops_web.i_calendars_owner_type_id ON shops_web.calendars (owner_id, type_id);


INSERT INTO shops_web.calendars (type_id, owner_id, name) VALUES (1, 225, 'Календарь праздников России');
INSERT INTO shops_web.calendars (type_id, owner_id, name) VALUES (1, 187, 'Календарь праздников Украины');
INSERT INTO shops_web.calendars (type_id, owner_id, name) VALUES (1, 149, 'Календарь праздников Республики Беларусь');
INSERT INTO shops_web.calendars (type_id, owner_id, name) VALUES (1, 159, 'Календарь праздников Казахстана');
INSERT INTO shops_web.calendars (type_id, owner_id, name) VALUES (4, 0, 'Дефолтный календарь служб доставки магазинов');


CREATE TABLE shops_web.calendar_day_types (
  id int PRIMARY KEY,
  calendar_type_id int NOT NULL REFERENCES shops_web.calendar_types (id),
  name varchar2(256) NOT NULL UNIQUE
);
CREATE INDEX shops_web.i_clndr_day_tps_clndr_tp_id ON shops_web.calendar_day_types (calendar_type_id);

INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (100, 2, 'Рабочий день службы доставки');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (110, 2, 'Нерабочий день службы доставки');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (200, 1, 'Праздничный день');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (210, 1, 'Рабочий выходной день');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (220, 1, 'Выходной день');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (230, 1, 'Рабочий день');
INSERT INTO shops_web.calendar_day_types (id, calendar_type_id, name) VALUES (240, 1, 'Выходной рабочий день');

CREATE TABLE shops_web.daily_calendars (
  calendar_id int NOT NULL REFERENCES shops_web.calendars (id),
  ymd date NOT NULL,
  type_id int NOT NULL REFERENCES shops_web.calendar_day_types (id),
  UNIQUE (calendar_id, ymd)
);
CREATE INDEX shops_web.i_daily_calendars_calendar_id ON shops_web.daily_calendars (calendar_id);
CREATE INDEX shops_web.i_daily_calendars_type_id ON shops_web.daily_calendars (type_id);
