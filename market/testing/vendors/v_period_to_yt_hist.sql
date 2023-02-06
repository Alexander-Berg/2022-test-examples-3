--liquibase formatted sql
--будет использоваться для выгрузки истории по вендорам и продуктам в Ыть для БТ
--changeset vladimir-k:VNDMARKET-1242_V_PERIOD_TO_YT_HIST_TESTING_2
CREATE OR REPLACE VIEW VENDORS.V_PERIOD_TO_YT_HIST AS
  SELECT
    to_date('2018-02-15', 'yyyy-mm-dd') AS FIRST_BACKLOAD_DATE,
    to_date('2018-02-17', 'yyyy-mm-dd') AS LAST_BACKLOAD_DATE
  FROM DUAL
