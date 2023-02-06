--liquibase formatted sql

--changeset yakun:MBI-32508_add_shops_web.v_shop_cpc/cpa_need_info_for_testing

INSERT INTO shops_web.environment (name,value)
VALUES ('shops_web.v_shop_cpc_need_info','shops_web.v_shop_cpc_need_info_test');

INSERT INTO shops_web.environment (name,value)
VALUES ('shops_web.v_shop_cpa_need_info','shops_web.v_shop_cpa_need_info_test');