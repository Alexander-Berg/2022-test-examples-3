--liquibase formatted sql

--changeset aefrem:MBI-42695-enable-mbi-log-processor
INSERT INTO shops_web.environment(name, value) VALUES('mbi-log-processor.enabled', 'true');
