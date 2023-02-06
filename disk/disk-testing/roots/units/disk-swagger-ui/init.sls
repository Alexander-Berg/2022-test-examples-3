{% set cluster = pillar.get('cluster') %}
{% set unit = 'disk-swagger-ui' %}

disk-swagger-ui:
  pkg:
    - installed

