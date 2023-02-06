{% set cluster = pillar.get('cluster') %}

include:
  - units.mpfs
  - units.mpfs.mpfs-core-uwsgi-disk
  - units.nginx
  - units.yandex-disk-log-reader
  - templates.mongodb.common
  - templates.certificates
  - units.monrun.restarts
  - units.loggiver
  - units.statbox-push-client
  - units.deploy-checks
