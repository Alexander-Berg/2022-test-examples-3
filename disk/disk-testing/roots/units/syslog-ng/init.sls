{% set cluster = pillar.get('cluster') %}

syslog-ng:
  service:
    - running
    - reload: True

{% for name,entry in pillar.get('syslog-config-files', {}).items() %}
{% for file in entry['files'] %}
{{file}}:
  yafile.managed:
    - source: salt://{{ entry['basedir'] }}{{ file }}
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in:
      - service: syslog-ng
{% endfor %}
{% endfor %}
