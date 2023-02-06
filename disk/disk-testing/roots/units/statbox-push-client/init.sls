{% set cluster = pillar.get('cluster') %}
{% set unit = 'statbox-push-client' %}

{% for file in pillar.get('statbox-push-client-daemon-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('statbox-push-client-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - follow_symlinks: True
{% endfor %}

{% for file in pillar.get('statbox-push-client-dirs', ['/var/spool/push-client']) %}
{{file}}:
  file.directory:
    - mode: 755
    - user: statbox
    - group: statbox
    - require:
      - group: statbox
      - user: statbox
{% endfor %}

statbox-push-client:
  service.enabled: []
  pkg.installed:
    - pkgs:
      - yandex-push-client
      - config-logrotate-push-client

# dir

statbox:
  user:
    - present
    - uid: 10000
    - gid: 10000
    - require:
      - group: statbox
  group:
    - present
    - gid: 10000

{% set logdir = pillar.get('statbox-push-client-log-dir', '/var/log/statbox/') %}
{{ logdir  }}:
  file.directory:
    - mode: 755
    - user: statbox
    - group: statbox
    - recurse:
      - user
      - group
      - mode
    - require:
      - group: statbox
      - user: statbox

{% if salt['pillar.get']('statbox-push-client_logs_status_check:enabled',False) %}
/etc/monrun/conf.d/push-client_logs_status.conf:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/monrun/conf.d/push-client_logs_status.conf
    - mode: 644
    - user: root
    - group: root

/usr/local/bin/push-client_logs_status.py:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/local/bin/push-client_logs_status.py
    - mode: 755
    - user: root
    - group: root

/etc/monitoring/push-client_logs_status.conf:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/monitoring/push-client_logs_status.conf
    - mode: 644
    - user: root
    - group: root
{% endif %}
