
cluster : disk_uploader

include:
  - units.yandex-disk-uploader
  - units.nginx
  - units.statbox-push-client
  # - units.esets

# =======================

disk_uploader-files:
  - /etc/tailer.conf
  - /etc/monitoring/unispace.conf
  - /etc/ImageMagick/policy.xml

disk_uploader-monrun-files:
  - /etc/monrun/conf.d/valid_traffic.conf
  - /etc/monrun/conf.d/cert_issuer.conf
  - /etc/monrun/conf.d/cert_date.conf

disk_uploader-exec-files:
  - /usr/bin/disk_uploader.traf.sh
  - /usr/bin/disk_uploader.valid_traffic.sh
  - /usr/bin/disk_uploader.get_certs.sh

disk_uploader-additional_pkgs:
  - psmisc
  - util-linux

disk_uploader-symlinks:
  /etc/nginx/sites-enabled/uploader.conf: /etc/nginx/sites-available/uploader.conf

# =======================

statbox-push-client-config-files:
  - /etc/yandex/statbox-push-client/push-client-uploader.yaml

# =======================
nginx-files:
  disk_uploader:
    basedir: files/disk_uploader
    files:
      - /etc/logrotate.d/nginx.conf

nginx-config-files:
  disk_uploader:
    basedir: files/disk_uploader
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/sites-available/uploader.conf

nginx-dirs:
  disk_uploader:
    dirs:
      - /var/log/nginx/uploader

nginx-monrun-files:
  - /etc/monrun/conf.d/nginx.conf


certificates:
  contents:
    disk_uploader.crt: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    disk_uploader.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    office.disk.yandex.net.crt: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    office.disk.yandex.net.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
  path: /etc/yandex/disk/uploader/keys/
  saltenv: testing
  packages: []
  cert_owner: root

mediastorage-mulcagate:
  http_check:
    service: 'disk_uploader_mulcagate'

{% set rand =  salt['random.seed'](1000,hash=salt['grains.get']('id').split('.')[0][:-1]) %}
random_test: {{ rand }}

tls_elliptics: {{salt.yav.get('sec-01dz16mcbhkbhhc3zbrfc7xnr7')|json}}
tls_karl: {{salt.yav.get('sec-01efkq40eze0j6ac96d0bjjp9d')|json}}

zk:
  username: uploader
  password: {{ salt.yav.get('sec-01d94mtzw1s6qhsrh3zzy7mb2h[zk_uploader_password]') | json }}
