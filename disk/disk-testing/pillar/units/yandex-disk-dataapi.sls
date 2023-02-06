{% set unit = 'yandex-disk-dataapi' %}

#{{ unit }}-files:
#  - /etc/cron.d/yandex-disk-dataapi-meta

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/yandex-disk-dataapi.conf

#{{ unit }}-exec-files:
#  - /usr/bin/dataapi.stat-sensors.py
#  - /usr/bin/dataapi.stat-access.py
#  - /usr/bin/dataapi.stat-events.py
#  - /usr/lib/yandex-graphite-checks/available/dataapi-sensors.sh
#  - /usr/lib/yandex-graphite-checks/available/dataapi-access.sh
#  - /usr/lib/yandex-graphite-checks/available/dataapi-events.sh

{{ unit }}-config-files:
  - /etc/yandex/dataapi/dataapi/application.properties
  - /etc/yandex/disk/dataapi/application.properties
    

#{{ unit }}-symlinks:
#  /usr/lib/yandex-graphite-checks/enabled/dataapi-events.sh: /usr/lib/yandex-graphite-checks/available/dataapi-events.sh
#  /usr/lib/yandex-graphite-checks/enabled/dataapi-access.sh: /usr/lib/yandex-graphite-checks/available/dataapi-access.sh
#  /usr/lib/yandex-graphite-checks/enabled/dataapi-sensors.sh: /usr/lib/yandex-graphite-checks/available/dataapi-sensors.sh


