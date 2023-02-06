{% set cluster = pillar.get('cluster') %}
{% set unit = 'drweb' %}

{% for file in pillar.get('drweb-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('drweb-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('drweb-syslog-ng-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: syslog-ng 
{% endfor %}

{% for file in pillar.get('drweb-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}


{% for file in pillar.get('drweb-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in:
      - service: drwebd
{% endfor %}

{% for link, target in pillar.get('drweb-symlinks').items() %}
{{ link }}: 
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}


drwebd:
  service:
    - running
    - require:
      - pkg: drwebd
  pkg:
    - installed
    - pkgs:
      - drweb-bases
      - drweb-common
      - drweb-daemon
      - drweb-libs
      - drweb-libs32
      - drweb-updater
