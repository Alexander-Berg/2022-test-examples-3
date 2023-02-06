{% set cluster = pillar.get('cluster') %}

include:
  - units.yandex-zookeeper-disk

