--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-628_create_mock_registry_table
CREATE TABLE VENDORS.MOCK_REGISTRY (
  NAME            VARCHAR2(100 CHAR) NOT NULL,
  STATUS          NUMBER NOT NULL,
  PARAMS          CLOB
) TABLESPACE VENDORS_DTS;

COMMENT ON TABLE VENDORS.MOCK_REGISTRY IS 'Конфигурация сервисов-заглушек';
COMMENT ON COLUMN VENDORS.MOCK_REGISTRY.NAME IS 'Имя интерфейса';
COMMENT ON COLUMN VENDORS.MOCK_REGISTRY.STATUS IS 'Статус заглушки (1 - использовать заглушку)';
COMMENT ON COLUMN VENDORS.MOCK_REGISTRY.PARAMS IS 'Параметры сервиса-заглушки (строка "key1=value1,key2=value2")';
--rollback DROP TABLE VENDORS.MOCK_REGISTRY;
