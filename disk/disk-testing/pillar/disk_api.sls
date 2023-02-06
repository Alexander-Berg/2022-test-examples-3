cluster : disk_api

include:
  - units.nginx
  - units.nginx-local-balancer
  - units.disk-swagger-ui

syslog-config-files:
  disk_api:
    basedir: files/disk_api
    files:
      - /etc/syslog-ng/conf-enabled/nginx-access.conf
      - /etc/syslog-ng/conf-enabled/nginx-error.conf

nginx-config-files:
  disk_api:
    basedir: files/disk_api
    files:
      - /etc/nginx/uwsgi_params_api
      - /etc/nginx/nginx.conf
      - /etc/nginx/ssl/https
      - /etc/nginx/ssl/intapi-https
      - /etc/nginx/ssl/yateam-https
      - /etc/nginx/sites-enabled/api.conf
      - /etc/nginx/sites-enabled/api-yateam.conf
      - /etc/nginx/sites-enabled/intapi.conf
      - /etc/nginx/sites-enabled/intapi-yateam.conf
      - /etc/nginx/sites-enabled/telemost-yateam.conf
      - /etc/nginx/sites-enabled/cloud-admin.conf
      - /etc/nginx/sites-enabled/20-loggiver3132.conf
      - /etc/nginx/conf.d/authnets.conf
      - /var/www/telemost-yateam/apple-app-site-association

nginx-files:
  disk_api:
    basedir: files/disk_api
    files:
      - /etc/logrotate.d/nginx.conf

nginx-dirs:
  disk_api:
    dirs:
      - /var/log/nginx/mpfs

certificates:
  contents:
    cloud-api.yandex.net.pem: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    cloud-api.yandex.net.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    intapi.disk.yandex.net.pem: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    intapi.disk.yandex.net.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    dst.yandex-team.ru.pem: {{ salt.yav.get('sec-01cryzmtjzhbn82q2c427h8gkq[pem]') | json }}
    dst.yandex-team.ru.key: {{ salt.yav.get('sec-01cryzmtjzhbn82q2c427h8gkq[key]') | json }}
  packages: []


disk_api-files:
  - /etc/yandex/mpfs/admins_overrides.yaml

{% set disk_secret_keys_filename = {
    'disk_test_api-mimino': 'disk-secret-keys.yaml-disk_test_api-mimino',
  }.get(
    grains.get("conductor")["group"],
    'disk-secret-keys.yaml-disk_test_api'
  )
%}

yarl:
  stage: disk-ratelimiter-testing-yarl
  du: yarl-root

disk-secret-keys: {{ salt.yav.get('sec-01crwhyj8a4jx7hmpm2ztqpdyy[' + disk_secret_keys_filename + ']') | json }}
tvm-asymmetric-public: {{ salt.yav.get('sec-01crwsz9abefdnf069kmevvpfe[tvm-asymmetric.public]') | json }}
