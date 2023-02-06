{% for file in pillar.get('logbackup-files') %}
{{file}}:
  file.managed:
    - source: salt://units/logbackup/files{{file}}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - dir_mode: 755
{% endfor %}

{% for file in pillar.get('logbackup-secret-files') %}
{{file}}:
  file.managed:
    - source: salt://units/logbackup/files{{file}}
    - mode: 400
    - user: root
    - group: root
    - makedirs: True
    - dir_mode: 755
{% endfor %}

{% for file in pillar.get('logbackup-exec-files') %}
{{file}}:
  file.managed:
    - source: salt://units/logbackup/files{{file}}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
    - dir_mode: 755
{% endfor %}

{% for dir in pillar.get('logbackup-dirs') %}
{{dir}}:
  file.directory:
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

logbackup:
  monrun.present:
    - execution_interval: 600
    - command: /usr/lib/yandex/disk/logbackup/check-logbackup.sh
