DROP VIEW IF EXISTS v_dbs_order_stat_testing;

CREATE OR REPLACE VIEW v_dbs_order_stat_testing AS
SELECT source.order_id                                                           AS order_id,
       shop_id,
       creation_date,

       (delivery_changed_by_shop
           OR
        coalesce(pickup, delivered, current_date) > estimated_delivery_date + make_interval(days := 1, hours := 12)
           OR (
                creation_date > '2022-03-14' AND
                coalesce(real_delivery_date, pickup, delivered, estimated_delivery_date) >=
                estimated_delivery_date + make_interval(days := 1)
            )
           )
           AND e.order_id IS NULL
           AND (estimated_delivery_date < '2022-02-24'
           OR
                estimated_delivery_date >= '2022-03-04'
           )
           AND (estimated_delivery_date < '2022-06-23'
           OR
                estimated_delivery_date >= '2022-06-24'
           )                                                                     AS late_delivery,

       (pending IS NOT NULL OR processing IS NOT NULL)
           AND (cancelled IS NULL OR
                cancelled >=
                estimated_delivery_date + make_interval(days := 1, hours := 12)) AS use_in_late_delivery,

       cancel_by_shop
           AND e.order_id IS NULL
           AND (cancelled < '2022-02-24' OR cancelled >= '2022-03-04')           AS cancel_by_shop,
       items_updated_by_shop
           AND NOT cancel_by_shop
           AND e.order_id IS NULL
           AND (items_update_date < '2022-02-24'
           OR
                items_update_date >= '2022-03-04')                               AS items_updated_by_shop,
       delivery IS NOT NULL OR cancel_by_shop OR items_updated_by_shop           AS use_in_cancel_by_shop,
       FALSE                                                                     AS return_by_shop,
       FALSE                                                                     AS arbitrage,
       coalesce(real_delivery_date, delivered, pickup)                           AS delivered_date,
       estimated_delivery_date                                                   AS estimated_delivery_date,
       delivery_changed_by_shop                                                  AS delivery_changed_by_shop,
       cancelled IS NOT NULL AND cancelled > estimated_delivery_date             AS cancelled_after_edd,
       cancelled                                                                 AS cancellation_date,
       cancelled_substatus                                                       AS cancellation_substatus,
       items_update_date                                                         AS items_update_date,
       items_update_reason                                                       AS items_update_reason,
       NULL :: timestamptz                                                       AS return_refund_date,
       NULL :: bigint[]                                                          AS return_reasons,
       NULL :: timestamptz                                                       AS arbitrage_verdict_date
FROM (
         SELECT cos.order_id                                                                 AS order_id,
                cos.shop_id                                                                  AS shop_id,
                cos.creation_date                                                            AS creation_date,
                coalesce(cod.rating_delivery_date, cod.by_order) :: date                     AS estimated_delivery_date,
                cod.rating_delivery_date IS NOT NULL                                         AS delivery_changed_by_shop,
                cos.pickup                                                                   AS pickup,
                cos.pending                                                                  AS pending,
                cos.processing                                                               AS processing,
                cos.cancelled                                                                AS cancelled,
                cos.cancelled_substatus                                                      AS cancelled_substatus,
                cos.delivery                                                                 AS delivery,
                cos.delivered                                                                AS delivered,
                cos.real_delivery_date                                                       AS real_delivery_date,
                items_update.order_id IS NOT NULL                                            AS items_updated_by_shop,
                items_update.items_update_time                                               AS items_update_date,
                items_update.items_update_reason_id                                          AS items_update_reason,
                coalesce(cos.cancelled_substatus, -1) IN (6, 9, 10, 17, 25)
                    OR (coalesce(cancelled_substatus, -1) = 11 AND cancelled > '2021-12-28') AS cancel_by_shop
                -- OrderSubstatus.SHOP_FAILED
                -- OrderSubstatus.PROCESSING_EXPIRED
                -- OrderSubstatus.PENDING_EXPIRED
                -- OrderSubstatus.MISSING_ITEM
                -- OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP
         FROM cpa_order_stat cos
                  JOIN cpa_order_delivery cod ON cos.order_id = cod.order_id
                  LEFT JOIN cpa_order_param items_update
                            ON items_update.order_id = cos.order_id
                                AND items_update.param_type = 'ITEMS_UPDATED_BY_PARTNER_FAULT'
         WHERE cos.creation_date BETWEEN current_date - 90 AND current_date
           AND cos.rgb = 4 -- Color.WHITE
     ) as source
         LEFT JOIN partner_rating_exclusion e
    -- такое условие для индекса
                   ON e.order_id = source.order_id
                       AND NOT e.deleted
                       AND e.exclusion_type = 'BY_ORDER'
