cluster: disk_mpfs_dev
{% set cluster = 'disk_mpfs_dev' %}

include: 
  - units.nginx

nginx-files:
  {{cluster}}:
    basedir: files/{{cluster}}
    files:
      - /etc/logrotate.d/nginx-mpfs

nginx-dirs:
  {{cluster}}:
    dirs:
      - /var/log/nginx/mpfs
      - /var/spool/nginx/cache

nginx-config-files:
  {{cluster}}:
    basedir: files/{{cluster}}
    files:
      - /etc/nginx/nginx.conf
      - /etc/nginx/uwsgi_params
      - /etc/nginx/sites-enabled/mpfs.conf
      - /etc/nginx/sites-enabled/api.conf
      - /etc/nginx/sites-enabled/intapi.conf
