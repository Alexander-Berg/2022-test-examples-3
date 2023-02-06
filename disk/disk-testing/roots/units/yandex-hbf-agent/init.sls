{% set unit = 'yandex-hbf-agent' %}


{% for file in pillar.get(unit + '-config-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - template: jinja
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


{% for file in pillar.get(unit + '-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

yandex-hbf-agent:
  service:
    - running
    - reload: True
    - require:
      - pkg: yandex-hbf-agent
  pkg:
    - installed
    - pkgs:
      - yandex-hbf-agent-static
      - yandex-hbf-agent-monitoring-static
      - hbf-agent-mds-config
      - hbf-agent-mds-config-ubic
      - python-psutil
