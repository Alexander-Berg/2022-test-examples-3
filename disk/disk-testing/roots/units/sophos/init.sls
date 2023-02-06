{% set cluster = pillar.get('cluster') %}
{% set unit = 'sophos' %}

{% for file in pillar.get('sophos-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('sophos-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('sophos-syslog-ng-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: syslog-ng 
{% endfor %}

{% for file in pillar.get('sophos-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}


{% for file in pillar.get('sophos-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in:
      - service: savdid
{% endfor %}

{% for link, target in pillar.get('sophos-symlinks').items() %}
{{ link }}: 
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}

{% for file in pillar.get('sophos-dirs') %}
{{file}}:
  file.directory:
    - mode: 755 
    - user: savdi
    - group: savdi
    - require:
      - group: savdi
      - user: savdi
{% endfor %}


savdid:
  service:
    - running
    - reload: True
    - require:
      - pkg: sophos
      - user: savdi
      - group: savdi

sophos:
  pkg.installed

savdi:
  user:
    - present
    - uid: 3434
    - gid: 3434
    - require:
      - group: savdi
  group:
    - present
    - gid: 3434
    - require:
      - pkg: sophos



