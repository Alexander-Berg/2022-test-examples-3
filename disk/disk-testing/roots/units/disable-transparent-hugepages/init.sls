{% set cluster = pillar.get('cluster') %}
{% set unit = 'disable-transparent-hugepages' %}

/etc/init.d/disable-transparent-hugepages:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/init.d/disable-transparent-hugepages
    - template: jinja
    - mode: 755
    - user: root
    - group: root
    - makedirs: True

disable-transparent-hugepages:
  service.running:
    - enable: True
    - require:
      - yafile: /etc/init.d/disable-transparent-hugepages


run-dth-defaults:
  cmd.run:
    - name: update-rc.d -f disable-transparent-hugepages defaults
    - unless: test -h /etc/rc2.d/S20disable-transparent-hugepages
    - require:
      - yafile: /etc/init.d/disable-transparent-hugepages
