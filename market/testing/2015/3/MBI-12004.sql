--liquibase formatted sql
--changeset saferif:MBI-12004_add_api_url_v2
update shops_web.environment set value = 'https://api-metrika.metrika-test.haze.yandex.ru' where name = 'metric.rest.apiUrl';
