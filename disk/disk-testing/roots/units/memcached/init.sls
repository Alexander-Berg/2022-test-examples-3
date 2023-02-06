{% set cluster = pillar.get('cluster') %}
{% set unit = 'memcached' %}

{% for file in pillar.get('memcached-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('memcached-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}


memcached:
  service:
    - running
    - require:
      - pkg: memcached
  pkg:
    - installed
    - pkgs:
      - memcached
