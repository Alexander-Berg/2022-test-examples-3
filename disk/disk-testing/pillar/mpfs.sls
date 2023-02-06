{% set unit = 'mpfs' %}

mpfs-common-files:
  - /etc/yandex/mpfs/admins_overrides.yaml
  - /etc/yandex/pingunoque/config.yaml
  - /etc/monitoring/watchdog.conf
  - /etc/monitoring/mpfs_queue_lifetime.conf
  - /etc/monitoring/postfix_queue.conf
  - /etc/cron.d/wd-mpfs-core-fcgi
  - /usr/lib/yandex/disk/mpfs/ensure_local_indexes.js
  - /var/lib/mpfs/mongo_stat.awk

mpfs-exec-files:
  - /usr/bin/mpfs_queue_lifetime
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-social.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-requests.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-queue.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-queue-error.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-nginx-access.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-requests-db.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-fcgi-error.py
  - /usr/lib/yandex-graphite-checks/enabled/mpfs-recount.py
  - /usr/lib/yandex/disk/mpfs/count_jobs.py
  - /usr/lib/yandex/disk/mpfs/check_old_jobs.sh
  - /usr/lib/yandex/disk/mpfs/pymongo-bson-zero-size.sh
  - /usr/bin/mpfsfcgi2tskv.pl
  - /usr/bin/mpfsaccess2tskv.pl
  - /usr/bin/mpfs-request2tskv.pl
  - /usr/bin/mpfsqueue-photoslice2tskv.pl
  - /usr/bin/fcgi-access2tskv.pl
  - /usr/bin/mpfsqueueall2tskv.pl
  - /usr/bin/mpfsbilling2tskv.pl
  - /usr/bin/mpfsqueue2tskv.pl
  - /usr/bin/mpfsindex2tskv.pl
  - /usr/lib/yandex/disk/mpfs/wd-mpfs-queue.sh

mpfs-extra-pkgs:
  - pigz
  - mongodb: '1:2.6.9.yandex1'
  - python-flask: '0.9-yandex1'
  - python-pymongo: '2.6.2-yandex6'
  - python-bson: '2.6.2-yandex6'

nginx-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/logrotate.d/nginx-mpfs

nginx-dirs:
  {{unit}}:
    dirs:
      - /var/log/nginx/mpfs
      - /var/spool/nginx/cache

nginx-config-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/sites-enabled/10-mpfs.disk.yandex.net.conf
