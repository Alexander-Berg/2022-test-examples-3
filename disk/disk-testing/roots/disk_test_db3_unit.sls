{% set cluster = pillar.get('cluster') %}

include:
  - templates.mongodb.common
  - templates.mongodb.replicaset
  - units.mongodb-mms-monitoring-agent

{% for file in pillar.get(cluster + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

