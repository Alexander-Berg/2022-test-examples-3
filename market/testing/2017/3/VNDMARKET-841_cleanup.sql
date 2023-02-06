--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-841_cleanup

DELETE FROM VENDORS.MODEL_EDITOR_REQUEST
WHERE REQUEST_DATA_ID IN (SELECT ID
                          FROM VENDORS.MODEL_EDITOR_REQUEST_DATA
                          WHERE REQUEST_DATA IS NOT NULL);

DELETE FROM VENDORS.MODEL_EDITOR_REQUEST_DATA
WHERE REQUEST_DATA IS NOT NULL;

ALTER TABLE VENDORS.MODEL_EDITOR_REQUEST_DATA
  DROP COLUMN REQUEST_DATA;

--rollback