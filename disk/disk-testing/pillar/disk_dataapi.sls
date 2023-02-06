cluster : disk_dataapi

include:
  - units.memcached
  - units.yandex-disk-dataapi
  - units.yandex-dataapi-meta
  - units.statbox-push-client
  - units.nginx

# =======================
nginx-files:
  disk_dataapi:
    basedir: files/disk_dataapi
    files:
      - /etc/logrotate.d/nginx.conf

nginx-config-files:
  disk_dataapi:
    basedir: files/disk_dataapi
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/sites-enabled/dataapi.conf
      - /etc/nginx/sites-enabled/datasync.conf

nginx-dirs:
  disk_dataapi:
    dirs:
      - /var/log/nginx/dataapi

nginx-monrun-files:
  - /etc/monrun/conf.d/nginx.conf

# ======= statbox-push-client =======

statbox-push-client-config-files:
  - /etc/yandex/statbox-push-client/push-client-dataapi.yaml

statbox-push-client-dirs:
  - /var/spool/push-client

statbox-push-client_logs_status_check:
  enabled: True
  
certificates:
  contents:
    etc/nginx/ssl/datasync.yandex.net.pem: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    etc/nginx/ssl/datasync.yandex.net.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
  path: ""
  packages: []
  cert_owner: disk

