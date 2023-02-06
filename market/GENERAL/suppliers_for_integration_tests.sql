--liquibase formatted sql

--changeset as14:MMDM-2828-insert-new-business-for-integration-tests
INSERT INTO mdm.supplier (id, name, supplier_type, real_supplier_id, business_id, crossdock_warehouse_id, related_warehouse_id, updated_ts, deleted, business_enabled, business_state_updated_ts, sales_models, business_switch_transports)
VALUES (114, 'МДМ тестовый бизнес для интеграционных. Скучно, зато понятно', 'BUSINESS', null, null, null, null, localtimestamp, false, false, localtimestamp, '[]', '{}');

--changeset as14:MMDM-2828-insert-new-supplier-for-integration-tests
INSERT INTO mdm.supplier (id, name, supplier_type, real_supplier_id, business_id, crossdock_warehouse_id, related_warehouse_id, updated_ts, deleted, business_enabled, business_state_updated_ts, sales_models, business_switch_transports)
VALUES (149, 'МДМ тестовый интеграицонный поставщик. Скучно, зато понятно.', 'THIRD_PARTY', null, 114, null, null, localtimestamp, false, false, localtimestamp, '["FULFILLMENT"]', '{}');
