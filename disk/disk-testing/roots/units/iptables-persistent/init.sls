{% set cluster = pillar.get('cluster') %}
{% set unit = 'iptables-persistent' %}

{% for file in pillar.get('iptables-persistent-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

iptables-persistent:
  service.enabled:
    - reload: True
    - require:
      - pkg: iptables-persistent
  pkg.installed:
    - pkgs:
      - iptables-persistent


