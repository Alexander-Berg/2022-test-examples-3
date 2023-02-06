{% set cluster = pillar.get('cluster') %}
{% set unit = 'yarl' %}

yarl_packages:
  pkg.installed:
    - pkgs:
      - yarl: 0.18
      - yandex-disk-reals-resolver: 0.1-6908599
      - gettext-base
    - require_in:
      - cmd: generate_config

/etc/yarl/yarl.tmpl:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/yarl/yarl.tmpl
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - makedirs: True

/usr/local/bin/yarl-conf-from-sd.sh:
  file.managed:
    - source: salt://{{ slspath }}/files//usr/local/bin/yarl-conf-from-sd.sh
    - mode: 755

generate_config:
  cmd.run:
    - name: /usr/local/bin/yarl-conf-from-sd.sh {{ salt['pillar.get']('yarl:stage') }} {{ salt['pillar.get']('yarl:du') }} > /etc/yarl/yarl.yaml
    - onchanges:
       - file: /etc/yarl/yarl.tmpl
    - require:
       - file: /etc/yarl/yarl.tmpl
       - file: /usr/local/bin/yarl-conf-from-sd.sh

/etc/yarl/nginx.yaml:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/yarl/nginx.yaml
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - makedirs: True

/etc/cron.d/yarl:
  file.absent

/etc/cron.d/yarl-update-root:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/cron.d/yarl-update-root
    - user: root
    - group: root
    - mode: 644

/usr/local/bin/yarl-update-root.sh:
  file.managed:
    - source: salt://{{ slspath }}/files/usr/local/bin/yarl-update-root.sh
    - template: jinja
    - mode: 755

/etc/logrotate.d/yarl:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/logrotate.d/yarl

/etc/monrun/conf.d/yarl.conf:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/monrun/conf.d/yarl.conf
    - template: jinja
    - makedirs: True

/etc/nginx/sites-enabled/30-ratelimiter.conf:
  file.managed:
    - source: salt://{{ slspath }}/files/etc/nginx/sites-enabled/30-ratelimiter.conf

/var/log/yarl:
  file.directory:
    - mode: 755
    - user: nginx
    - group: nginx
 
/usr/local/bin/yarl-check.py:
  file.managed:
    - source: salt://{{ slspath }}/files/usr/local/bin/yarl-check.py
    - template: jinja
    - makedirs: True
    - mode: 755

yarl-errors:
  file.managed:
    - source: salt://{{ slspath }}/files/usr/local/bin/yarl-errors.sh
    - name: /usr/local/bin/yarl-errors.sh
    - mode: 755
    - makedirs: True
  monrun.present:
    - command: /usr/local/bin/yarl-errors.sh
    - execution_interval: 300
    - execution_timeout: 30
    - type: yarl

include:
  - .ubic
