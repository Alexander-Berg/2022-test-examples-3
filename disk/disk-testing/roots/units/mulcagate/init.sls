{% set cluster = pillar.get('cluster') %}
{% set unit = 'mulcagate' %}

mulcagate:
  service:
    - running
    - require:
      - pkg: mulcagate
  pkg:
    - installed
    - pkgs:
      - butil
      - elliptics-client
      - handystats
      - libmulca
      - mulca-system
      - mulcagate
      - ymod-httpserver
      - yplatform
      - yandex-ubic-mulcagate
      - yandex-media-common-mulcagate-conf
      - language-pack-ru

