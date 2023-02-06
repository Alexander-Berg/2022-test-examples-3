{% set unit = 'yandex-disk-uploader' %}

{{ unit }}-files:
  - /etc/cron.d/yandex-disk-uploader-meta

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/uploader-sensors-stats.conf
  - /etc/monrun/conf.d/uploader-events-stats.conf
  - /etc/monrun/conf.d/uploader-access-stats.conf

{{ unit }}-exec-files:
#TODO: move  - /usr/bin/disk_uploader.get_certs.sh
  - /usr/bin/disk_uploader.uploader-sensors.py
  - /usr/bin/disk_uploader.uploader-access.py
  - /usr/bin/disk_uploader.uploader-events.py

  - /usr/lib/yandex-graphite-checks/available/uploader-events.sh
  - /usr/lib/yandex-graphite-checks/available/uploader-access.sh
  - /usr/lib/yandex-graphite-checks/available/uploader-access-preview-1s.sh
  - /usr/lib/yandex-graphite-checks/available/uploader-sensors.sh

{{ unit }}-config-files:
  - /etc/yandex/disk/uploader/application.properties

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/uploader-events.sh: /usr/lib/yandex-graphite-checks/available/uploader-events.sh
  /usr/lib/yandex-graphite-checks/enabled/uploader-access.sh: /usr/lib/yandex-graphite-checks/available/uploader-access.sh
  /usr/lib/yandex-graphite-checks/enabled/uploader-access-preview-1s.sh: /usr/lib/yandex-graphite-checks/available/uploader-access-preview-1s.sh
  /usr/lib/yandex-graphite-checks/enabled/uploader-sensors.sh: /usr/lib/yandex-graphite-checks/available/uploader-sensors.sh


