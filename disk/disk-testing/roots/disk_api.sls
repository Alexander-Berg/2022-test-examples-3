{% set cluster = pillar.get('cluster') %}

include:
  - units.nginx
  - units.nginx.tskv
  - units.nginx-local-balancer
  - templates.certificates
  - units.syslog-ng
  - units.disk-swagger-ui
  - units.deploy-checks
  - units.yarl

monrun-mpfs-api:
  monrun.present:
    - name: mpfs-api
    - execution_interval: 10
    - command: '/usr/bin/http_check.sh ping 80'

monrun-mpfs-intapi:
  monrun.present:
    - name: mpfs-intapi
    - execution_interval: 10
    - command: '/usr/bin/http_check.sh ping 8080'

mpfs-extapi-uwsgi:
    service:
          - running

mpfs-intapi-uwsgi:
    service:
          - running

monrun-lighttpd_3132:
  monrun.present:
    - name: lighttpd_3132
    - file: /etc/monrun/conf.d/lighttpd3132.conf
    - execution_interval: 300
    - command: '/usr/bin/http_check.sh ping_pattern 3333'

{% for file in pillar.get(cluster + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


/etc/nginx/keys/tvm-asymmetric.public:
  file.managed:
    - contents: {{ pillar['tvm-asymmetric-public'] | json }}
    - mode: 440 
    - user: root
    - group: nginx
    - makedirs: True

/etc/yandex/disk-secret-keys.yaml:
  file.managed:
    - contents: {{ pillar['disk-secret-keys'] | json }}
    - mode: 440
    - user: root
    - group: nginx
    - makedirs: True
