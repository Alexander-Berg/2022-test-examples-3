DROP VIEW IF EXISTS v_dropship_order_stat_testing;

-- esd - estimated_shipment_date
CREATE OR REPLACE VIEW v_dropship_order_stat_testing AS
-- Логика расчета всех составляющих, кроме возвратов по вине магазина
SELECT shop_id,
       src.order_id,
       CASE
           WHEN esd >= '2022-02-24' and esd < '2022-03-04' THEN 0
           WHEN esd >= '2022-06-23' and esd < '2022-06-24' THEN 0
           WHEN is_excluded_order THEN 0
           -- своевременность сборки экспресс заказа
           WHEN is_express AND cancelled IS NOT NULL AND cancelled <= esd_upper THEN 0
           WHEN is_express AND coalesce(shipped_time, current_date) > esd_upper THEN 1
           WHEN is_express THEN 0
           -- своевременность отгрузки обычного дропшип заказа
           -- отмененные до ПДО заказы считаем своевременными
           WHEN cancelled IS NOT NULL AND date_trunc('day', cancelled) <= date_trunc('day', esd_upper) THEN 0
           -- перенос даты отгрузки магазином
           WHEN is_shipment_date_changed AND esd >= '2022-03-18' THEN 1
           -- ранняя отгрузка(c&c)
           WHEN is_click_n_collect AND shipped_time < date_trunc('day', esd_upper) THEN 1
           -- поздняя отгрузка(c&c)
           WHEN is_click_n_collect AND date_trunc('day', shipped_time) > esd_upper + make_interval(days := 1) THEN 1
           -- ранняя отгрузка(fbs)
           WHEN NOT is_click_n_collect AND shipped_time < esd_lower THEN 1
           -- поздняя отгрузка(fbs)
           WHEN NOT is_click_n_collect AND shipped_time > esd_upper THEN 1
           -- если не проставлены и shipped и delivery, то заказ считаем отгруженным несвоевременно
           WHEN (shipped_time IS NULL AND (delivery IS NULL OR is_click_n_collect)) THEN 1
           ELSE 0 END                                                                  AS late_shipped,

       CASE
           WHEN esd >= '2022-02-24' and esd < '2022-03-04' THEN 0
           WHEN esd >= '2022-06-23' and esd < '2022-06-24' THEN 0
           WHEN is_excluded_order THEN 0
           WHEN NOT is_express AND shipped_time IS NULL
               AND cancelled IS NOT NULL AND date_trunc('day', cancelled) > date_trunc('day', esd_upper)
               THEN 1
           WHEN is_express AND shipped_time IS NULL AND cancelled IS NOT NULL AND cancelled > esd_upper THEN 1
           ELSE 0 END                                                                  AS not_shipped_and_cancelled_after_esd,

       CASE
           WHEN esd >= '2022-02-24' and esd < '2022-03-04' THEN 0
           WHEN esd >= '2022-06-23' and esd < '2022-06-24' THEN 0
           WHEN is_excluded_order THEN 0
           WHEN NOT is_express AND shipped_time IS NULL AND cancelled IS NULL AND current_date > esd_upper THEN 1
           WHEN is_express AND shipped_time IS NULL AND cancelled IS NULL AND current_date > esd_upper THEN 1
           ELSE 0 END                                                                  AS not_shipped_after_esd,

       CASE
           WHEN processing IS NULL THEN FALSE
           WHEN is_express THEN shipped_time IS NOT NULL OR coalesce(cancelled, current_date) > esd_upper
           ELSE shipped_time IS NOT NULL OR
                date_trunc('day', coalesce(cancelled, current_date)) > date_trunc('day', esd_upper)
           END                                                                         AS use_in_late_ship_rate,
       cancelled,
       cancelled_substatus,
       items_update_time,
       items_update_reason,
       CASE
           WHEN is_excluded_order THEN 0
           WHEN cancelled_by_shop
               AND (cancelled < '2022-02-24' OR cancelled >= '2022-03-04') THEN 1
           WHEN items_updated_by_shop
               AND (items_update_time < '2022-02-24' OR items_update_time >= '2022-03-04') THEN 1
           ELSE 0 END                                                                  AS shop_failed,
       cancelled_by_shop
           AND (NOT is_excluded_order)
           AND (cancelled < '2022-02-24' OR cancelled >= '2022-03-04')                 AS cancelled_by_shop,
       items_updated_by_shop
           AND (NOT cancelled_by_shop)
           AND (NOT is_excluded_order)
           AND (items_update_time < '2022-02-24' OR items_update_time >= '2022-03-04') AS items_updated_by_shop,
       shipped_time IS NOT NULL OR cancelled_by_shop OR items_updated_by_shop          AS use_in_cancellation_rate,
       delivery,
       delivered,
       esd,
       esd_upper,
       shipped_time,
       NULL ::BIGINT                                                                   AS order_id_with_return,
       NULL                                                                            AS return_item_reasons,
       NULl ::TIMESTAMPTZ                                                              AS return_refund_time,
       is_dropoff,
       is_excluded_order,
       is_shipment_date_changed,
       CASE
           WHEN is_click_n_collect THEN 'CLICK_AND_COLLECT'
           ELSE 'DSBB'
           END                                                                         AS partner_model,
       is_express                                                                      AS is_express
FROM (SELECT cos.order_id,
             cos.shop_id,
             cos.processing,
             cos.delivery,
             cos.delivered,
             cos.cancelled,
             cod.by_shipment,
             cos.cancelled_substatus,
             coalesce(cancelled_substatus, -1) IN (6, 10, 17, 25)
                 OR (coalesce(cancelled_substatus, -1) = 11 AND cancelled > '2021-12-22')                                       AS cancelled_by_shop,
             -- OrderSubstatus.SHOP_FAILED
             -- OrderSubstatus.PENDING_EXPIRED
             -- OrderSubstatus.MISSING_ITEM
             -- OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP
             -- OrderSubstatus.SHOP_PENDING_CANCELLED
             items_update.order_id IS NOT NULL                                                                                  AS items_updated_by_shop,
             items_update.items_update_time                                                                                     AS items_update_time,
             items_update.items_update_reason_id                                                                                AS items_update_reason,
             CASE
                 WHEN delivery_partner_type = 'SHOP' THEN coalesce(pickup, delivery)
                 WHEN express IS TRUE THEN date_trunc('minute', ready_to_ship)
                 ELSE coalesce(
                         least(
                                 loc_10.checkpoint_real_time,
                                 loc_110.checkpoint_time +
                                 make_interval(hours := coalesce(r_110.tz_offset - 3, 0)::integer),
                                 loc_120.checkpoint_time +
                                 make_interval(hours := coalesce(r_120.tz_offset - 3, 0)::integer),
                                 loc_130.checkpoint_time +
                                 make_interval(hours := coalesce(r_130.tz_offset - 3, 0)::integer)
                             ),
                         delivery)
                 END                                                                                                            AS shipped_time,
             CASE
                 WHEN delivery_partner_type = 'SHOP' THEN coalesce(rating_shipment_date, by_shipment)
                 WHEN express IS TRUE THEN express_estimated.express_estimated_time
                 WHEN dropoff IS TRUE THEN date_trunc('day', coalesce(rating_shipment_date, by_shipment)) +
                                           make_interval(hours := 15)
                 WHEN intake IS TRUE AND estimated_intake_time IS NOT NULL THEN estimated_intake_time
                 WHEN coalesce(rating_shipment_date, by_shipment) < '2022-07-12'
                     THEN date_trunc('day', coalesce(rating_shipment_date, by_shipment)) +
                          make_interval(days := 1, hours := 10)
                 ELSE date_trunc('day', coalesce(rating_shipment_date, by_shipment)) +
                      make_interval(days := 1, hours := 8)
                 END                                                                                                            AS esd_upper,
             CASE
                 WHEN delivery_partner_type = 'SHOP' THEN coalesce(rating_shipment_date, by_shipment)
                 WHEN express IS TRUE THEN express_estimated.express_estimated_time
                 WHEN dropoff IS TRUE THEN date_trunc('day', coalesce(rating_shipment_date, by_shipment)) -
                                           make_interval(hours := 10)
                 ELSE date_trunc('day', coalesce(rating_shipment_date, by_shipment)) + make_interval(hours := 8)
                 END                                                                                                            AS esd_lower,
             CASE
                 WHEN express IS TRUE THEN express_estimated.express_estimated_time
                 ELSE coalesce(rating_shipment_date, by_shipment)
                 END                                                                                                            AS esd,
             cod.delivery_partner_type = 'SHOP'                                                                                 AS is_click_n_collect,
             cod.express IS TRUE                                                                                                AS is_express,
             dropoff IS TRUE                                                                                                    AS is_dropoff,
             rating_shipment_date IS NOT NULL                                                                                   AS is_shipment_date_changed,
             cos.order_id IN (SELECT order_id
                              FROM partner_rating_exclusion
                              WHERE NOT deleted
                                AND exclusion_type = 'BY_ORDER') OR
             (coalesce(rating_shipment_date, by_shipment) :: date, shop_id) IN (SELECT excluded_date, partner_id
                                                                                FROM partner_rating_exclusion
                                                                                WHERE NOT deleted
                                                                                  AND exclusion_type = 'BY_ESTIMATED_SHIPMENT') AS is_excluded_order
      FROM cpa_order_stat cos
               JOIN cpa_order_delivery cod ON cod.order_id = cos.order_id
               LEFT JOIN lom_order lo ON cos.order_id = lo.checkouter_order_id
               LEFT JOIN cpa_order_param express_estimated
                         ON cos.order_id = express_estimated.order_id
                             AND express_estimated.param_type = 'EXPRESS_ORDER_ESTIMATED_TIME'
               LEFT JOIN cpa_order_param items_update
                         ON items_update.order_id = cos.order_id
                             AND items_update.param_type = 'ITEMS_UPDATED_BY_PARTNER_FAULT'
               LEFT JOIN lom_order_checkpoint loc_10
                         on cos.order_id = loc_10.order_id AND loc_10.checkpoint_type_id = 10
               LEFT JOIN lom_order_checkpoint loc_110
                         on cos.order_id = loc_110.order_id AND loc_110.checkpoint_type_id = 110
               LEFT JOIN region r_110 ON loc_110.location_id = r_110.id
               LEFT JOIN lom_order_checkpoint loc_120
                         on cos.order_id = loc_120.order_id AND loc_120.checkpoint_type_id = 120
               LEFT JOIN region r_120 ON loc_120.location_id = r_120.id
               LEFT JOIN lom_order_checkpoint loc_130
                         on cos.order_id = loc_130.order_id AND loc_130.checkpoint_type_id = 130
               LEFT JOIN region r_130 ON loc_130.location_id = r_130.id
      WHERE cos.creation_date BETWEEN current_date - make_interval(days := 90)
          AND current_date - make_interval(days := 1)
        -- Marketplace id
        AND cos.shop_id != 431782
        -- Color.BLUE
        AND cos.rgb = 1
     ) AS src
