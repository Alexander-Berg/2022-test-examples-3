{% set cluster = pillar.get('cluster') %}

include:
  - units.erateserver
  - units.loggiver
  - templates.ipvs_tun


