{% set cluster = pillar.get('cluster') %}

include:
  - units.statbox-push-client
  - units.fastcgi-blackbox-authorizer
  - units.syslog-ng
  - templates.certificates
  - units.yandex-disk-downloader
  - units.nginx
#  - templates.mediastorage-mulcagate
#  - templates.lepton

{% for file in pillar.get( 'disk_downloader-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get( 'disk_downloader-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('disk_downloader-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for link, target in pillar.get('disk_downloader-symlinks').items() %}
{{ link }}: 
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
{% endfor %}

/etc/tvmtool/tvmtool.conf:
  file.managed:
    - contents: {{ pillar['tvmtool-conf'] | json }}
    - mode: 600 
    - user: www-data
    - group: root
    - makedirs: True

monrun_tvmtool_check:
  monrun.present:
    - name: tvmtool
    - type: tvm
    - execution_interval: 30
    - command: '/usr/bin/jhttp.sh -s http -p 1488 -u /tvm/ping'

{% for pkg in pillar.get('disk_downloader-additional_pkgs') %}
{{pkg}}:
  pkg:
    - installed
{% endfor %}

/etc/yandex/disk-secret-keys.yaml:
  file.managed:
    - contents: {{ pillar['disk-secret-keys'] | json }}
    - mode: 440
    - user: root
    - group: root
    - makedirs: True

