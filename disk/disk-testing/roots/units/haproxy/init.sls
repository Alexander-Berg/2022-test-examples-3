{% set cluster = pillar.get('cluster') %}
{% set unit = 'haproxy' %}

{% for name,entry in pillar.get('haproxy-files', {}).items() %}
{% for file in entry['files'] %}
{{file}}:
  yafile.managed:
    - source: salt://{{ entry['basedir'] }}{{ file }}
    - template: jinja
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}
{% endfor %}

{% for file in pillar.get('haproxy-monrun-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for name,entry in pillar.get('haproxy-config-files', {}).items() %}
{% for file in entry['files'] %}
{{file}}:
  yafile.managed:
    - source: salt://{{ entry['basedir'] }}{{ file }}
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: 
      - service: haproxy
{% endfor %}
{% endfor %}

{% for name,entry in pillar.get('haproxy-dirs', {}).items() %}
{% for dir in entry['dirs'] %}
{{dir}}:
  file.directory:
    - mode: 755
    - user: haproxy
    - group: haproxy
    - makedirs: True
    - require:
        - pkg: haproxy
        - user: haproxy
        - group: haproxy
{% endfor %}
{% endfor %}

{% for name,entry in pillar.get('haproxy-symlinks', {}).items() %}
{% for link,target in entry['symlinks'].items() %}
{{ link }}: 
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}
{% endfor %}

haproxy:
  service:
    - running
    - reload: True
    - require:
      - pkg: haproxy
  user:
    - present
    - system: True
    - groups:
      - haproxy
    - require:
      - group: haproxy
  group:
    - present
    - system: True
  pkg:
    - installed

