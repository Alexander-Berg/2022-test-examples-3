{% set cluster = pillar.get('cluster') %}

include:
  - units.yandex-hbf-agent
  - units.pingunoque