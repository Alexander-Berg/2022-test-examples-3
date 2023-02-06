--liquibase formatted sql

--changeset skiftcha:MBI-19214-view
CREATE OR REPLACE
VIEW market_billing.v_test_shipments
(
    campaign_id,
    cpc_sum_to_ship,
    cpa_sum_to_ship,
    sum_to_ship,
    cpa_orders_sum_to_ship,
    cpa_orders_to_ship,
    shipment_date
)
AS
  SELECT
    ns.campaign_id,
    cpc_sum_to_ship,
    cpa_sum_to_ship,
    sum_to_ship,
    cpa_orders_sum_to_ship,
    cpa_orders_to_ship,
    shipment_date
  FROM market_billing.v_new_shipments_for_balance ns
    JOIN market_billing.campaign_info ci ON ns.campaign_id = ci.campaign_id
  WHERE ci.datasource_id in (SELECT shop_id FROM market_billing.test_shops);
