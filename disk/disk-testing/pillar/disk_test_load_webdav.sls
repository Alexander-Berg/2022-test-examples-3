cluster : disk_test_load_webdav

include:
  - units.mpfs
  - units.yadrop
  - units.statbox-push-client

disk_webdav-files:
  - /var/www/share/dist/Certum.p7b

disk_webdav-extra-pkgs:
  - htop

nginx-files:
  disk-webdav:
    basedir: files/disk_webdav
    files:
      - /etc/nginx/sites-enabled/10-webdav.yandex.ru.conf

certificates:
  contents:
    webdav.yandex.ru.pem: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[pem]') | json }}
    webdav.yandex.ru.key: {{ salt.yav.get('sec-01crx3h1fsq6w1kbd8hdyp30y5[key]') | json }}
    YandexInternalCA.pem: {{ salt.yav.get('sec-01crz1js872tjx9ypct2xf8pbp[pem]') | json }}
    CertumCA.pem: {{ salt.yav.get('sec-01crz1mn3chz0q6gdcasf0bn40[pem]') | json }}
  path: /etc/yamail/ssl
  saltenv: testing
  packages: []
  cert_owner: yadrop


statbox-push-client-config-files:
  - /etc/yandex/statbox-push-client/push-client-webdav.yaml

statbox-push-client-dirs:
  - /var/spool/push-client


