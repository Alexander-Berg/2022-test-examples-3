{% set cluster = pillar.get('cluster') %}
{% set unit = 'pingunoque' %}

{% for file in pillar.get(unit + '-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}