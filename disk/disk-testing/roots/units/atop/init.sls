{% set cluster = pillar.get('cluster') %}
{% set unit = 'atop' %}

{% for file in pillar.get('atop-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

atop:
  service:
    - running
    - require:
       - pkg: atop
  pkg:
    - installed
