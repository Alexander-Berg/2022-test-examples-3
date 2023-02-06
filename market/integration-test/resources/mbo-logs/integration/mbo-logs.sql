CREATE TABLE `generations_test` (
  `session_id` varchar(50) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `status` enum('new','in_process','published','failed','skipped') DEFAULT NULL,
  `added_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `start_time` timestamp NULL DEFAULT NULL,
  `finished_time` timestamp NULL DEFAULT NULL,
  `rowcount` int(11) DEFAULT '-1',
  `indexer_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`table_name`,`session_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8
