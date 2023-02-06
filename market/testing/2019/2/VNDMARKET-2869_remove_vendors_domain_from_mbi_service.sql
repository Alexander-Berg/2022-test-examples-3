--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-2869_remove_vendors_domain_from_mbi_service_test
DELETE FROM JAVA_SEC.SERVICES_DOMAINS WHERE DOMAIN_ID = 17148402;
