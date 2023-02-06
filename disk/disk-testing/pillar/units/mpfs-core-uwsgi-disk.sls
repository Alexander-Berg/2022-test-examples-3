{% set unit = 'mpfs' %}


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
      - /etc/nginx/ssl/https.conf
      - /etc/nginx/conf.d/authnets.conf


