{% set unit = 'yandex-zookeeper-disk' %}

{{ unit }}-config-files:
  - /etc/yandex/zookeeper-disk/zoo.cfg
  - /etc/yandex/zookeeper-disk/java.env

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/yandex-zookeeper-disk.conf

{{ unit }}-exec-files:
  - /usr/local/yandex/monitoring/zk_followers.sh
  - /usr/local/yandex/monitoring/zk_mntr.sh
  - /usr/local/yandex/monitoring/zk_mntr_is_ok.sh
  - /usr/local/yandex/monitoring/zk_server_role.sh
