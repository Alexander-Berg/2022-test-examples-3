{% set unit = 'squid3' %}

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/squid.conf
  - /etc/monrun/conf.d/squid-access-stats.conf

{{ unit }}-exec-files:
  - /usr/bin/squid.stat-access.py
  - /usr/lib/yandex-graphite-checks/available/squid-access.sh

{{ unit }}-config-files:
  - /etc/squid3/squid.conf

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/squid-access.sh: /usr/lib/yandex-graphite-checks/available/squid-access.sh

