{% set cluster = pillar.get('cluster') %}
{% set unit = 'nginx-local-balancer' %}

{% for name,entry in pillar.get(unit + '-config-files', {}).items() %}
{% for file in entry['files'] %}
{{file}}:
  yafile.managed:
    - source: salt://{{ entry['basedir'] }}{{ file }}
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - template: jinja
    - watch_in: 
      - service: nginx
{% endfor %}
{% endfor %}

/etc/hosts.manual:
    yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/hosts.manual
    - template: jinja
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - template: jinja
