CREATE OR REPLACE VIEW market_billing.monitor_test_shop_free_orders AS
  WITH t1 AS (
      SELECT shop_id, COUNT(*) AS cnt
      FROM market_billing.cpa_order
      WHERE shop_id != 774
        AND free = 0
        AND trantime < TO_DATE('2013-11-20', 'yyyy.MM.dd')
      GROUP BY shop_id
  ),
  t AS (
    SELECT
      LISTAGG(shop_id, ',') WITHIN GROUP (ORDER BY shop_id) AS shops,
      SUM(cnt) AS cnt
    FROM t1
  ) SELECT
      CASE
        WHEN t.cnt > 0 THEN 1
        ELSE 0
      END AS result,
      CASE
        WHEN t.cnt > 0 THEN 'Shops ' || t.shops || ' have in total ' || t.cnt || ' non-free cpa orders'
        ELSE NULL
      END AS description
    FROM t;

DROP VIEW  market_billing.monitor_test_shop_free_orders
/