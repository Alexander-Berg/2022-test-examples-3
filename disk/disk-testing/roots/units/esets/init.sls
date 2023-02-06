{% set cluster = pillar.get('cluster') %}
{% set unit = 'esets' %}

{% for file in pillar.get(unit + '-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get(unit + '-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get(unit + '-syslog-ng-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: syslog-ng 
{% endfor %}

{% for link, target in pillar.get(unit + '-symlinks').items() %}
{{ link }}:
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}

{% for file in pillar.get(unit + '-license-files', {}) %}
/etc/opt/eset/esets/license/{{ file }}:
  file.decode:
    - contents_pillar: "{{ unit }}-license-files:{{ file }}"
    - require:
      - pkg: esets
    - require_in:
      - service: esets
{% endfor %}

{% for file in pillar.get(unit + '-license-files', {}) %}
/etc/opt/eset/esets/license/{{ file }}_permissions:
  file.managed:
    - name: /etc/opt/eset/esets/license/{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - replace: False
    - makedirs: True
    - require:
      - pkg: esets
    - require_in:
      - service: esets
{% endfor %}

/etc/opt/eset/esets/esets.cfg:
  file.managed:
    - contents_pillar: "{{ unit }}-esets-cfg"
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - require:
      - pkg: esets
    - require_in:
      - service: esets

esets:
  service:
    - running
    - reload: True
    - require:
      - pkg: esets
  pkg:
    - installed
  monrun.present:
    - command: '/usr/bin/esets-check-icap.py'
    - execution_interval: 60
    - execution_timeout: 30
    - type: esets

esets-bases-date:
  monrun.present:
    - command: '/usr/bin/esets-check-bases-date.sh'
    - execution_interval: 60
    - execution_timeout: 30
    - type: esets

esets-key-date:
  monrun.present:
    - command: '/usr/bin/esets-check-key-date.sh'
    - execution_interval: 60
    - execution_timeout: 30
    - type: esets

