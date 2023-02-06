--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-3083-fix_analytics_tariffs endDelimiter:///

BEGIN
    UPDATE CS_BILLING.TARIFF_PARAM tp SET tp.NUM_VALUE = 60000000 WHERE tp.TARIFF_ID = 24 AND tp.NAME = 'MIN_SUM';
END;
