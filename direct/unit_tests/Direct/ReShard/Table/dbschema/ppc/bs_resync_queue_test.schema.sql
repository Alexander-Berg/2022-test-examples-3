CREATE TABLE `bs_resync_queue_test` (
  `Id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `cid` int(10) unsigned NOT NULL,
  `bid` bigint(20) NOT NULL,
  `pid` int(10) unsigned NOT NULL DEFAULT '0',
  `sequence_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` tinyint(4) NOT NULL DEFAULT '0',
  `Priority_Inverse` tinyint(4) GENERATED ALWAYS AS (-(`priority`)) VIRTUAL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cid` (`cid`,`bid`,`pid`),
  KEY `priority_inverse` (`priority_inverse`,`sequence_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
