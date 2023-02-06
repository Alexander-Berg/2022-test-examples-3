
cluster : disk_downloader

include:
  - units.nginx
  - units.yandex-disk-downloader
  - units.statbox-push-client
  - units.fastcgi-blackbox-authorizer

# ====== disk_downloader =======

disk_downloader-files:
  - /etc/tailer.conf
  - /etc/monitoring/unispace.conf
  - /etc/free_space_watchdog/free_space_watchdog.conf
  - /var/lib/yandex-disk-downloader-hostlist/disk_downloader-net
  - /etc/yandex/loggiver/loggiver.pattern
  - /usr/share/yandex-configs/disk_downloader/downloader-tskv-access.exclude

disk_downloader-monrun-files:
  - /etc/monrun/conf.d/valid_traffic.conf
  - /etc/monrun/conf.d/cert_issuer.conf
  - /etc/monrun/conf.d/cert_date.conf

disk_downloader-exec-files:
  - /usr/share/free_space_watchdog/iptables-close-nginx.sh
  - /usr/bin/disk_downloader.get_certs.sh

disk_downloader-additional_pkgs:
  - psmisc
  - util-linux

disk_downloader-symlinks:
  /etc/nginx/sites-enabled/hostlist.conf: /etc/nginx/sites-available/hostlist.conf
#  /etc/yamail/ssl/disk.yandex.ru.crt: /etc/yamail/ssl/downloader.dsd.yandex.ru.crt
#  /etc/yamail/ssl/disk.yandex.ru.key: /etc/yamail/ssl/downloader.dsd.yandex.ru.key

# ======= statbox-push-client =======

statbox-push-client-config-files:
  - /etc/yandex/statbox-push-client/push-client-downloader.yaml 


# ======= nginx ======= #

nginx-files:
  disk_downloader:
    basedir: files/disk_downloader
    files:
      - /etc/logrotate.d/nginx.conf

nginx-config-files:
  disk_downloader:
    basedir: files/disk_downloader
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/disk/include/proxy_cache_path.include 
      - /etc/nginx/sites-available/hostlist.conf

nginx-dirs:
  disk_downloader:
    dirs:
      - /var/log/nginx/downloader
      - /u0/nginx/tmp
      - /u0/nginx/cache_preview
      - /u0/nginx/cache

nginx-monrun-files:
  - /etc/monrun/conf.d/nginx.conf

certificates:
  contents:
    disk.yandex.ru.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    disk.yandex.ru.crt: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
  path: /etc/yamail/ssl/
  packages: []
  saltenv: testing
  cert_group: root

tvmtool-conf: {{ salt.yav.get('sec-01crtc4cgckjv1nv5m2afpe8k9[tvmtool.conf]') | json }}
disk-secret-keys: {{ salt.yav.get('sec-01crwhyj8a4jx7hmpm2ztqpdyy[disk-secret-keys.yaml]') | json }}

mediastorage-mulcagate:
  http_check:
    service: 'disk_downloader_mulcagate'
  tls:
    support: 1

tls_elliptics: {{salt.yav.get('sec-01dz16mcbhkbhhc3zbrfc7xnr7')|json}}
tls_karl: {{salt.yav.get('sec-01efkq40eze0j6ac96d0bjjp9d')|json}}
