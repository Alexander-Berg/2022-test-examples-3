--liquibase formatted sql
--changeset vlepihin:MBI-20789

delete from shops_web.environment where name = 'environment.type';
insert into shops_web.environment(name, value) values('environment.type', 'testing');