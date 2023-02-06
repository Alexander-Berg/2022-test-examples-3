{% set cluster = pillar.get('cluster') %}


{% for file in pillar.get('mpfs-sync-common-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('mpfs-sync-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}



mpfs-core-uwsgi-browser:
  service:
    - running
    - reload: False

mpfs-queue-sync:
  service:
    - running
    - reload: False

mpfs-queue-browser:
  service:
    - running
    - reload: False

mpfs-sync:
  monrun.present:
    - command: '/usr/bin/http_check.sh ping 80'
    - execution_interval: 10



