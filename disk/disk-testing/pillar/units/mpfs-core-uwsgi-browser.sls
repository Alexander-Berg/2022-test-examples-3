{% set unit = 'mpfs' %}


mpfs-sync-common-files:
  - /etc/yandex/mpfs/admins_overrides.yaml
  - /etc/cron.d/wd-mpfs-queue
  - /etc/cron.d/wd-mpfs-core-fcgi


mpfs-sync-exec-files:
  - /usr/bin/mpfs-stat-http-requests.py
  - /usr/bin/mpfs-queue-stats.py
  - /usr/bin/mpfs-stat-mongodb-requests.py
  - /usr/lib/yandex/disk/mpfs/count_jobs_sync.py


