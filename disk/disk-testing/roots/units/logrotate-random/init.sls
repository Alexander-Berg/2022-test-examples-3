{% set cluster = pillar.get('cluster') %}
{% set unit = 'logrotate-random' %}

/etc/cron.d/logrotate-hourly:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/cron.d/logrotate-hourly
    - mode: 644
    - user: root
    - group: root
