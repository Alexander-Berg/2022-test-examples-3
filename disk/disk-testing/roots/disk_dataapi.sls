{% set cluster = pillar.get('cluster') %}

include:
  - units.memcached
  - units.yandex-disk-dataapi
  - units.yandex-dataapi-meta
  - units.statbox-push-client
  - units.nginx
  - units.logrotate-random
  - templates.certificates

/etc/yabs-chkdisk-stop:
  file.absent

