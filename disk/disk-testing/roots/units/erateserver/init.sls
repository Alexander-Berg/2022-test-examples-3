{% set cluster = pillar.get('cluster') %}
{% set unit = 'erateserver' %}

{#
{% for file in pillar.get('erateserver-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}
#}

{% for file in pillar.get('erateserver-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in:
      - service: erateserver
{% endfor %}

{#
{% for file in pillar.get('erateserver-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}
#}

{% for file in pillar.get('erateserver-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}


erateserver:
  pkg:
    - installed
  service:
    - running
    - require:
      - pkg: erateserver
      - user: erateserver
      - group: erateserver
  user:
    - present
    - system: True
    - createhome: True
    - home: '/var/lib/erateserver'
    - groups:
      - erateserver
    - require:
      - group: erateserver
  group: 
    - present
    - system: True



