--liquibase formatted sql

--changeset ivanov-af:DELIVERY-14229-insert-perf-testing-consumer
INSERT INTO DELIVERY_TRACKER.CONSUMER(ID, NAME)
VALUES (3, 'perf-testing-mock');
