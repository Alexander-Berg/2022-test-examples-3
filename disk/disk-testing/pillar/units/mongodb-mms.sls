{% set unit = 'mongodb-mms' %}


{{ unit }}-exec-files:
  - /opt/mongodb/mms/bin/mongodb-mms

{{ unit }}-config-files:
  - /opt/mongodb/mms/conf/conf-mms.properties
  - /etc/mongodb-mms/gen.key


### nginx

nginx-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/logrotate.d/nginx-opsm

nginx-dirs:
  {{unit}}:
    dirs:
      - /var/log/nginx/opsm
      - /var/spool/nginx/cache

nginx-config-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/sites-available/opsm.conf

nginx-symlinks:
  {{unit}}:
    symlinks:
      /etc/nginx/sites-available/opsm.conf: /etc/nginx/sites-enabled/opsm.conf

