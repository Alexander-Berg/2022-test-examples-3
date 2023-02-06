--liquibase formatted sql

--changeSet wadim:MBI-add-creation_date-column-to-datasources_testing_result
alter table shops_web.datasources_testing_result add creation_date date default sysdate not null;

--changeSet wadim:MBI-21770
drop table shops_web.datasources_testing_result;