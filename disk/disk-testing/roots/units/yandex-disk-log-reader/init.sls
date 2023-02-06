{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-disk-log-reader' %}


{% for file in pillar.get('yandex-disk-log-reader-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

yandex-disk-log-reader:
  service:
    - running
  pkg:
    - installed

