cluster: disk_mpfs

include:
  - units.mpfs
  - units.mpfs-core-uwsgi-disk
  - units.nginx
  - units.statbox-push-client
  - units.yandex-disk-log-reader

# ======= statbox-push-client =======

statbox-push-client-config-files:
  - /etc/yandex/statbox-push-client/push-client-mpfs.yaml

statbox-push-client-dirs:
  - /var/spool/push-client

statbox-push-client_logs_status_check:
  enabled: False

disk_mpfs-files:
  - /etc/cron.d/mpfs-queue-restart

nginx-files:
  disk-mpfs:
    basedir: files/disk_mpfs
    files:
      - /etc/nginx/conf.d/01-mpfs-tskv-log.conf

certificates:
  contents:
    mpfs.disk.yandex.net.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    mpfs.disk.yandex.net.pem: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
  packages: []

mongodb:
  python3: False

