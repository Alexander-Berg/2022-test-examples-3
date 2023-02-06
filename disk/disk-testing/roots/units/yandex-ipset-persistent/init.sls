{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-ipset-persistent' %}

{% for file in pillar.get('yandex-ipset-persistent-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

ipset-persistent:
  service.enabled:
    - reload: True
    - require:
      - pkg: ipset-persistent
  pkg.installed:
    - pkgs:
      - yandex-ipset-persistent
      - ipset
