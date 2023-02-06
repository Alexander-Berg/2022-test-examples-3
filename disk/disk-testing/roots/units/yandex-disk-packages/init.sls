{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-disk-packages' %}

{% for pkgname in pillar.get(unit + '-packages') %}
{{pkgname}}:
  pkg.installed
{% endfor %}


/etc/juggler/client.conf:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/juggler/client.conf
    - mode: 644
    - user: root
    - group: root
    - watch_in:
      - service: juggler-client

juggler-client:
  pkg:
    - installed
  service:
    - running

/usr/bin/salt-highstate:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/bin/salt-highstate
    - mode: 755
    - user: root
    - group: root

/usr/bin/salt-sls:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/bin/salt-sls
    - mode: 755
    - user: root
    - group: root

/usr/bin/salt-sls-id:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/bin/salt-sls-id
    - mode: 755
    - user: root
    - group: root

/usr/bin/salt-minion-check.sh:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/bin/salt-minion-check.sh
    - mode: 755
    - user: root
    - group: root

salt-minion:
  service.dead:
    - enable: False
