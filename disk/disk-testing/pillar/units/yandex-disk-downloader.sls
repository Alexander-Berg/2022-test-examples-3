{% set unit = 'yandex-disk-downloader' %}

{{ unit }}-files:
  - /etc/cron.d/yandex-disk-downloader-meta
  - /etc/logrotate.d/yandex-disk-downloader.conf

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/nginx-stats.conf

{{ unit }}-exec-files:
  - /usr/bin/disk_downloader.url2stid.pl
  - /usr/bin/disk_downloader.nginx-access.py
  - /usr/bin/disk_downloader.clean-tmp.sh
  - /usr/bin/downloader.stat-tskv-access.py
  - /usr/lib/yandex-graphite-checks/available/downloader-access-preview-1s.sh
  - /usr/lib/yandex-graphite-checks/available/downloader-access.sh

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/downloader-access.sh: /usr/lib/yandex-graphite-checks/available/downloader-access.sh
  /usr/lib/yandex-graphite-checks/enabled/downloader-access-preview-1s.sh: /usr/lib/yandex-graphite-checks/available/downloader-access-preview-1s.sh

