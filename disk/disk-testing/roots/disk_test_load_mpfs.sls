{% set cluster = pillar.get('cluster') %}

include:
  - units.mpfs
  - units.mpfs.mpfs-core-uwsgi-disk
  - units.nginx
  - units.monrun.restarts
  - units.deploy-checks
  - units.pingunoque
