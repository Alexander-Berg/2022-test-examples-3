{% set cluster = pillar.get('cluster') %}
{% set unit = 'fastcgi-blackbox-authorizer' %}


{% for file in pillar.get('fastcgi-blackbox-authorizer-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('fastcgi-blackbox-authorizer-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('fastcgi-blackbox-authorizer-syslog-ng-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - watch_in: syslog-ng 
{% endfor %}

{% for file in pillar.get('fastcgi-blackbox-authorizer-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for link, target in pillar.get('fastcgi-blackbox-authorizer-symlinks').items() %}
{{ link }}:
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}

fastcgi-blackbox-authorizer:
  service:
    - running
    - require:
      - pkg: fastcgi-blackbox-authorizer
  pkg:
    - installed
    - pkgs:
      - fastcgi-daemon2
      - fastcgi-blackbox-authorizer
      - fastcgi-daemon2-ubic
      - fastcgi-regional-cdn
      - elliptics-regional-module
      - libauth-client-parser
      - libfastcgi-daemon2
      - libfastcgi2-syslog
      - url-checker

