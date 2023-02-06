{% set cluster = pillar.get('cluster') %}
{% set unit = 'mulcagate-rdr-conf' %}

# temporary state for dev deploy

mulcagate-rdr-conf:
  pkg:
    - installed
    - pkgs:
      - yandex-media-common-mulcagate-rdr-conf


