{% set cluster = pillar.get('cluster') %}
{% set unit = 'mulcagate-plain-conf' %}

# temporary state for dev deploy

mulcagate-plain-conf:
  pkg:
    - installed
    - pkgs:
      - yandex-media-common-mulcagate-plain-conf


