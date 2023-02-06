{% set unit = 'disk-swagger-ui' %}

nginx-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/logrotate.d/nginx-disk-swagger-ui

nginx-dirs:
  {{unit}}:
    dirs:
      - /var/log/nginx/disk-swagger-ui
      - /var/spool/nginx/cache

nginx-config-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/nginx/sites-enabled/disk-swagger-ui.conf

