{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-selfdns-client' %}


{% for file in pillar.get(unit + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - template: jinja
{% endfor %}


/etc/yandex/selfdns-client/default.conf:
  file.managed:
    - source: salt://units/{{ unit }}/files/etc/yandex/selfdns-client/default.conf
    - mode: 440
    - user: root
    - group: selfdns
    - template: jinja
    - makedirs: True
    - require:
      - pkg: yandex-selfdns-client

disable-ipv6-privacy-extensions:
  file.absent:
    - name: /etc/sysctl.d/10-ipv6-privacy.conf

yandex-selfdns-client:
  pkg.installed:
    - version: 0.2.18

lldpd:
  pkg:
    - installed

iproute2:
  pkg:
    - installed
