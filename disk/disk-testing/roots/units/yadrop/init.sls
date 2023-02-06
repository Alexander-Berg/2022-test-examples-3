{% set cluster = pillar.get('cluster') %}
{% set unit = 'yadrop' %}


{% for file in pillar.get(unit + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


{% for file in pillar.get(unit + '-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

yadrop-http:
  monrun.present:
    - execution_interval: 10
    - command: '/usr/bin/jhttp.sh -s http -p 8088 -u /?ping'

yadrop-internal:
  monrun.present:
    - execution_interval: 10
    - command: '/usr/bin/jhttp.sh -s http -p 8089 -u /?ping'

yadrop:
  service:
    - running
    - require:
      - user: yadrop
  user:
    - present
    - system: True
    - shell: '/sbin/nologin'
    - createhome: True
    - home: '/var/lib/yadrop'
    - groups:
      - yadrop
    - require:
      - group: yadrop
  group: 
    - present
    - system: True

