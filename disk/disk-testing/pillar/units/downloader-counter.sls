
downloader-counter-files:
  - /etc/cron.d/yandex-disk-downloader-counter

downloader-counter-monrun-files:
  - /etc/monrun/conf.d/downloads-stats.conf

downloader-counter-exec-files:
  - /usr/bin/disk_downloader.downloads-counter.pl
  - /usr/bin/disk_downloader.count-reporter.pl
  - /usr/lib/yandex-graphite-checks/available/downloads-counter.sh

downloader-counter-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/downloads-counter.sh: /usr/lib/yandex-graphite-checks/available/downloads-counter.sh

