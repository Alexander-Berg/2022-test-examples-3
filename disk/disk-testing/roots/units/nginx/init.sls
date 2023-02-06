{% set cluster = pillar.get('cluster') %}
{% set unit = 'nginx' %}

{% for name,entry in pillar.get('nginx-files', {}).items() %}
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

{% for file in pillar.get('nginx-monrun-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for name,entry in pillar.get('nginx-config-files', {}).items() %}
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

{% for name,entry in pillar.get('nginx-dirs', {}).items() %}
{% for dir in entry['dirs'] %}
{{dir}}:
  file.directory:
    - mode: 755
    - user: nginx
    - group: nginx
    - makedirs: True
    - require:
        - pkg: nginx
        - user: nginx
        - group: nginx
{% endfor %}
{% endfor %}

{% for name,entry in pillar.get('nginx-symlinks', {}).items() %}
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


nginx:
  service:
    - running
    - reload: True
    - require:
      - pkg: nginx
  user:
    - present
    - system: True
    - groups:
      - nginx
    - require:
      - group: nginx
  group:
    - present
    - system: True
  pkg:
    - installed
    - pkgs:
      - nginx
      - nginx-common
      - nginx-full

