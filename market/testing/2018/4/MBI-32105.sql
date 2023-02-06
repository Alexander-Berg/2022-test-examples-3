--liquibase formatted sql

--changeset yakun:MBI-32508_remove_values

DELETE FROM shops_web.environment WHERE name IN ('shops_web.v_shop_cpc_need_info', 'shops_web.v_shop_cpa_need_info');