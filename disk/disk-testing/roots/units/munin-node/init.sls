{% set cluster = pillar.get('cluster') %}
{% set unit = 'munin-node' %}

{% for name,entry in pillar.get('munin-node-config-files', {}).items() %}
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


{% for name,entry in pillar.get('munin-node-symlinks', {}).items() %}
{% for link,target in entry['symlinks'].items() %}
{{ link }}: 
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
{% endfor %}
{% endfor %}


/etc/munin/plugins/:
  file.directory:
    - clean: True
    - require:
{% for name,entry in pillar.get('munin-node-symlinks', {}).items() %}
{% for link,target in entry['symlinks'].items() %}
      - file: {{ link }}
{% endfor %}
{% endfor %}




munin-node:
  service:
    - running
    - require:
      - pkg: munin-node
  pkg:
    - installed
    - pkgs:
      - munin-node

