{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-disk-dataapi' %}


{% for file in pillar.get(unit + '-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for file in pillar.get(unit + '-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


yandex-disk-dataapi:
  service:
    - running
  pkg:
    - installed

