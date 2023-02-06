--liquibase formatted sql

--changeset vbudnev:MBI-18571-MARKET_BILLING-MST_RCV_QUEUE

MERGE INTO MARKET_BILLING.MST_RCV_QUEUE dst_queue
USING (
        SELECT 'market-report-generator-billing' AS receiver_name, 'plog_click' AS table_name FROM dual UNION ALL
        SELECT 'market-report-generator-billing', 'click_rollback' FROM dual UNION ALL
        SELECT 'marketbill-ng', 'plog_show' FROM dual
      ) src
ON (dst_queue.receiver_name = src.receiver_name AND dst_queue.table_name = src.table_name AND dst_queue.status = 3 )
WHEN NOT MATCHED THEN
INSERT (id, receiver_name, table_name, status, from_trans_id, to_trans_id, fails_count)
VALUES (MARKET_BILLING.S_MST_RCV_QUEUE.nextval, src.receiver_name, src.table_name, 3, 0, 1 , 0 );

--changeset nongi:MBI-57228-REMOVE-BOOKNOW

MERGE INTO MB_STAT_REPORT.MST_RCV_QUEUE dst_queue
USING (
          SELECT 'market-report-generator-stats' AS receiver_name,	'plog_show' AS table_name FROM dual UNION ALL
          SELECT 'market-report-generator-stats',	'plog_click' FROM dual UNION ALL
          SELECT 'market-report-generator-stats',	'click_rollback' FROM dual UNION ALL
          SELECT 'market-report-generator-stats',	'show_rollback' FROM dual UNION ALL
          SELECT 'market-report-generator-stats',	'cpa_clicks' FROM dual UNION ALL
          SELECT 'market-report-generator-stats',	'cpa_clicks_rollback' FROM dual
        ) src
ON (dst_queue.receiver_name = src.receiver_name AND dst_queue.table_name = src.table_name AND dst_queue.status = 3 )
WHEN NOT MATCHED THEN
INSERT (id, receiver_name, table_name, status, from_trans_id, to_trans_id, fails_count)
VALUES (MB_STAT_REPORT.S_MST_RCV_QUEUE.nextval, src.receiver_name, src.table_name, 3, 0, 1, 0 );

