{% set cluster = pillar.get('cluster') %}

include:
  - templates.mongodb.common
  - templates.mongodb.replicaset
  - units.mongodb-mms-monitoring-agent
