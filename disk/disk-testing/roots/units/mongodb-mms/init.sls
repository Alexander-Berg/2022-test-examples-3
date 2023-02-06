{% set cluster = pillar.get('cluster') %}
{% set unit = 'mongodb-mms' %}


{% for file in pillar.get(unit + '-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755 
    - user: mongodb-mms
    - group: mongodb-mms
    - makedirs: True
{% endfor %}


{% for file in pillar.get(unit + '-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 600
    - user: mongodb-mms
    - group: mongodb-mms
    - makedirs: True
{% endfor %}


mongodb-mms:
  pkg:
    - installed
    - pkgs:
      - mongodb-mms


mongodb-mms-alive:
  monrun.present:
    - execution_interval: 60
    - execution_timeout: 10
    - command: '/usr/bin/jhttp.sh --server localhost --port 8080 --url /ping'
    - type: mongodb


