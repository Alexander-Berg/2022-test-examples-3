--liquibase formatted sql
--changeset nastik:MBI-15097_testing_vendor_service_product endDelimiter:;
UPDATE cs_billing.param_value SET num_value = 507013 where cs_id = 132 and param_type_id = 5;