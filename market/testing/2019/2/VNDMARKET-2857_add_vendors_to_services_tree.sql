--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-2857_add_vendors_to_services_tree_test
INSERT INTO JAVA_SEC.SERVICES_DOMAINS (SERVICE_ID, DOMAIN_ID)
VALUES (2, 17148402);
