--liquibase formatted sql
--changeset inal2i:MARKETCHECKOUT-21302 runInTransaction:false
drop index concurrently if exists i_order_change_request_created_at;
create index concurrently if not exists i_order_change_request_created_at on order_change_requests (created_at);
