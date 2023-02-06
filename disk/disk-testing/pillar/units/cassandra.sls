{% set unit = 'cassandra' %}

{{ unit }}-files:
  - /etc/cassandra-backup/cassandra-backup.conf
  - /usr/share/cassandra/lib/metrics-graphite-2.2.0.jar
  - /usr/share/cassandra/lib/mx4j-tools.jar 
  - /etc/monitoring/cassandra-mx4j-disk_ks-page_view_counts-write-99thpercentile.conf
  - /etc/monitoring/cassandra-mx4j-disk_ks-page_view_counts-read-99thpercentile.conf
  - /etc/monitoring/cassandra-ntstatus-state.conf
  - /etc/monitoring/cassandra-nttpstats-drops.conf
  - /etc/monitoring/cassandra-mx4j-system-hints-mccount.conf
  - /etc/monitoring/cassandra-nttpstats-threadpools.conf
  - /etc/monitoring/cassandra-mx4j-disk_ks-page_view_counts-write-95thpercentile.conf
  - /etc/monitoring/cassandra-mx4j-disk_ks-page_view_counts-read-95thpercentile.conf
  - /etc/monitoring/cassandra-ntstatus-status.conf
  - /etc/monitoring/cassandra-ntstatus-owns.conf

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/cassandra-mx4j-disk_ks-page_view_counts-write-99thpercentile.conf
  - /etc/monrun/conf.d/cassandra-mx4j-disk_ks-page_view_counts-read-99thpercentile.conf
  - /etc/monrun/conf.d/cassandra-mx4j-disk_ks-page_view_counts-write-95thpercentile.conf
  - /etc/monrun/conf.d/cassandra-mx4j-disk_ks-page_view_counts-read-95thpercentile.conf
  - /etc/monrun/conf.d/cassandra.conf

{{ unit }}-exec-files:
  - /usr/lib/yandex-graphite-checks/available/cassandra.sh

{{ unit }}-config-files:
  - /etc/cassandra/cassandra-env.sh
  - /etc/cassandra/cassandra.yaml
  - /etc/cassandra/monitoring.yaml
  - /etc/cassandra/cassandra-topology.properties
  - /etc/cassandra/log4j-server.properties

{{ unit }}-dirs:
  - /var/lib/cassandra
  - /opt/cassandra

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/cassandra.sh: /usr/lib/yandex-graphite-checks/available/cassandra.sh

