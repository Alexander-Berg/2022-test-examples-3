--liquibase formatted sql

--changeset kudrale:MBI-16657
MERGE INTO READER.CURRENCY_RATE dst
USING
  (
    SELECT
      7                               SOURCE_ID,
      'RUR'                           CODE,
      to_date('00-01-01', 'RR-mm-dd') EVENTTIME
    FROM dual
  ) src
ON (src.EVENTTIME = dst.EVENTTIME AND src.SOURCE_ID = dst.SOURCE_ID AND src.CODE = dst.CODE)
WHEN NOT MATCHED THEN
INSERT (ID, SOURCE_ID, CODE, NOMINAL, VALUE, EVENTTIME, TRANTIME)
VALUES (READER.S_CURRENCY_RATE.NEXTVAL, 7, 'RUR', 30, 1, to_date('00-01-01', 'RR-mm-dd'), sysdate);

MERGE INTO READER.CURRENCY_RATE dst
USING
  (
    SELECT
      7                                   SOURCE_ID,
      'UAH'                               CODE,
      to_date('2014-09-01', 'yyyy-mm-dd') EVENTTIME
    FROM dual
  ) src
ON (src.EVENTTIME = dst.EVENTTIME AND src.SOURCE_ID = dst.SOURCE_ID AND src.CODE = dst.CODE)
WHEN NOT MATCHED THEN
INSERT (ID, SOURCE_ID, CODE, NOMINAL, VALUE, EVENTTIME, TRANTIME)
VALUES (READER.S_CURRENCY_RATE.NEXTVAL, 7, 'UAH', 12, 1, to_date('2014-09-01', 'yyyy-mm-dd'), sysdate);

