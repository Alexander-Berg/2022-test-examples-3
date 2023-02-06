--liquibase formatted sql

--changeset anmokretsov:MBI-39916-restore-constraint-testing-cpa_order_item_click_info-1
alter table market_billing.cpa_order_item_click_info drop constraint pk_cpa_oi_clk_info;

--changeset anmokretsov:MBI-39916-restore-constraint-testing-cpa_order_item_click_info-2
alter table market_billing.cpa_order_item_click_info add constraint pk_cpa_oi_clk_info primary key (order_id, feed_id, offer_id);

--changeset anmokretsov:MBI-39916-restore-constraint-testing-cpa_order_item_click_info-3
drop index market_billing.ix_cpa_oi_clk_info_order_id;

--changeset anmokretsov:MBI-39916-restore-constraint-testing-cpa_order_item_click_info-4
alter table market_billing.cpa_order_item_click_info modify item_id null;
