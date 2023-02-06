create schema sch23;

create table sch23.PARTNER
(
  ID int,
  constraint pk_sch_par_id primary key (ID)
);

create table sch23.CALENDAR_OWNER_KEYS
(
  CALENDAR_OWNER_ID int,
  KEY_TYPE int,
  KEY_NUMERIC_VALUE int,
  constraint pk_sch_calownkey_calownid_keytyp primary key (calendar_owner_id, key_type)
);

create table sch23.CALENDARS (
  ID int,
  OWNER_ID int,
  constraint pk_sch_cal_id primary key (ID)
);
