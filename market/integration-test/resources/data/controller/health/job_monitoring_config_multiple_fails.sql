INSERT INTO job_monitoring_config (job_name, max_delay_time, max_execution_time, max_failed_run) VALUES
('fullSyncExecutor', 1200000, 1800000, 1),
('korobyteSyncExecutor', 1200000, 1800000, 1),
('hourlyReportStockStateExecutor', 1200000, 1800000, 4),
('unknownJob', 1200000, 1800000, 1);
