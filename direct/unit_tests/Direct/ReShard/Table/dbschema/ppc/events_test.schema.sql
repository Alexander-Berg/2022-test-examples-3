CREATE TABLE `events_test` (
  `eventobject` enum('camp','adgroup','banner','phrase') NOT NULL DEFAULT 'camp',
  `eventtype` enum('c_start','c_finish') DEFAULT NULL,
  `eventtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `objectid` bigint(20) unsigned NOT NULL DEFAULT '0',
  `objectuid` bigint(20) unsigned NOT NULL DEFAULT '0', 
  `eid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uid` bigint(20) unsigned NOT NULL DEFAULT '0',
  `cid` int(10) unsigned NOT NULL DEFAULT '0',
  `json_data` text NOT NULL,
  PRIMARY KEY (`eid`),
  KEY `i_objectuid` (`objectuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
