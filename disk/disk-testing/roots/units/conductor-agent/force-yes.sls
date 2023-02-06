{% set cluster = pillar.get('cluster') %}
{% set unit = 'conductor-agent' %}


/etc/conductor-agent/conf.d/10-disk.conf:
  yafile.managed:
    - source: salt://units/conductor-agent/files/etc/conductor-agent/conf.d/10-disk.conf
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True

