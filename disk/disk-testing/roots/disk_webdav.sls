include: 
  - units.yadrop
  - units.nginx
  - templates.certificates
  - templates.mediastorage-mulcagate
  - templates.lepton



{% for file in pillar.get('disk_webdav-files') %}
{{file}}:
  file.managed:
    - source: salt://files/disk_webdav{{file}}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for pkg in pillar.get('disk_webdav-extra-pkgs') %}
{{pkg}}:
  pkg:
    - installed
{% endfor %}


cert_expires:
  monrun.present:
    - execution_interval: 300
    - command: '/usr/lib/config-monrun-cert-check/cert_check_extended.sh -h localhost -p 8443 -e 30'

cert_issuer:
  monrun.present:
    - execution_interval: 30
    - command: '/usr/lib/config-monrun-cert-check/cert_check_extended.sh -h localhost -p 8443 -i Yandex'

nginx-http:
  monrun.present:
    - execution_interval: 10
    - command: '/usr/bin/jhttp.sh -s http -p 8400 -u /ping'

