{% set cluster = pillar.get('cluster') %}
{% set unit = 'cassandra' %}


{% for file in pillar.get('cassandra-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('cassandra-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('cassandra-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for file in pillar.get('cassandra-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('cassandra-dirs') %}
{{file}}:
  file.directory:
    - user: cassandra
    - group: cassandra
    - mode: 755 
{% endfor %}

{% for link, target in pillar.get('cassandra-symlinks').items() %}
{{ link }}:
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}

cassandra:
  service:
    - running
  pkg:
    - installed

