{% set cluster = pillar.get('cluster') %}
{% set unit = 'mongodb-mms-monitoring-agent' %}


{% for file in pillar.get(unit + '-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 600 
    - user: mongodb-mms-agent
    - group: mongodb-mms-agent
    - makedirs: True
{% endfor %}


{% for file in pillar.get(unit + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}



mongodb-mms-monitoring-agent:
  service:
    - running
    - require:
      - pkg: mongodb-mms-monitoring-agent
  pkg:
    - installed
    - sources:
      - mongodb-mms-monitoring-agent: https://opsm.qloud.dst.yandex.net/download/agent/monitoring/mongodb-mms-monitoring-agent_6.1.2.402-1_amd64.deb


mongodb-mms-monitoring-agent-alive:
  monrun.present:
    - execution_interval: 60
    - execution_timeout: 10
    - command: '/usr/lib/config-monitoring-common/daemon_check.sh mongodb-mms-monitoring-agent'
    - type: mongodb


