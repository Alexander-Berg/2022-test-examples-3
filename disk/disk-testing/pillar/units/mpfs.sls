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
  - /usr/lib/yandex/disk/mpfs/mpfs_exception_stat.py
  - /usr/lib/yandex/disk/mpfs/check_old_jobs.sh
  - /usr/lib/yandex/disk/mpfs/pymongo-bson-zero-size.sh
  - /usr/lib/yandex/disk/mpfs/wd-mpfs-queue-master-available.sh
  - /usr/lib/yandex/disk/mpfs/mpfs-queue-master-available.sh
  - /usr/bin/mpfsfcgi2tskv.pl
  - /usr/bin/mpfsaccess2tskv.pl
  - /usr/bin/mpfs-request2tskv.pl
  - /usr/bin/mpfsqueue-photoslice2tskv.pl
  - /usr/bin/fcgi-access2tskv.pl
  - /usr/bin/mpfsqueueall2tskv.pl
  - /usr/bin/mpfsbilling2tskv.pl
  - /usr/bin/mpfsqueue2tskv.pl
  - /usr/bin/mpfsindex2tskv.pl
  - /usr/bin/nginx-error2tskv.pl
  - /usr/lib/yandex/disk/mpfs/wd-mpfs-queue.sh
  - /usr/bin/mpfs-stat-http-requests.py
  - /usr/bin/mpfs-queue-stats.py
  - /usr/bin/mpfs-stat-mongodb-requests.py

mpfs-extra-pkgs:
  - pigz
  - mongodb: '1:2.6.9.yandex1'
  - python-pymongo: '2.6.3-yandex1'
  - python-bson: '2.6.3-yandex1'
  - python-kazoo: '2.6.1-yandex1'


mpfs-queue2-files:
   - /etc/cron.d/wd-mpfs-queue2

mpfs-queue2-exec-files:
   - /usr/lib/yandex/disk/mpfs/wd-mpfs-queue2.sh

disk-secret-keys: {{ salt.yav.get('sec-01crwhyj8a4jx7hmpm2ztqpdyy[disk-secret-keys.yaml]') | json }}
tvm-asymmetric-public: {{ salt.yav.get('sec-01crwtprtzz0kztq3aghe49jyj[tvm-asymmetric.public]') | json }}
disk-mpfs-token: {{ salt.yav.get('sec-01crwtprtzz0kztq3aghe49jyj[disk-mpfs-token]') | json }}

{% set access_overrides_filename = {
    'disk_test_mpfs-current': 'access_overrides.yaml-disk_test_mpfs-current',
    'disk_test_mpfs-hotfix': 'access_overrides.yaml-disk_test_mpfs-hotfix',
    'disk_test_mpfs-manual': 'access_overrides.yaml-disk_test_mpfs-manual',
    'disk_test_mpfs-stable': 'access_overrides.yaml-disk_test_mpfs-stable',
  }.get(
    grains.get("conductor")["group"],
    'access_overrides.yaml'
  )
%}

access-overrides-yaml: {{ salt.yav.get('sec-01crwtprtzz0kztq3aghe49jyj[' + access_overrides_filename + ']') | json }}
