DELETE FROM loads WHERE dictionary LIKE '%test_promo_data_to_yt_csv%';
DELETE FROM loads WHERE dictionary LIKE '%test_2_promo_data_to_yt_csv%';
DELETE FROM cron_task WHERE task LIKE '%test_promo_data_to_yt_csv%';
DELETE FROM cron_task WHERE task LIKE '%test_2_promo_data_to_yt_csv%';
