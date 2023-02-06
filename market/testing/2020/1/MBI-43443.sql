--liquibase formatted sql

--changeset serenitas:MBI-43343
update shops_web.outlet_info
set type = 'DEPOT'
where delivery_service_id in (213, 214) and datasource_id = -1;