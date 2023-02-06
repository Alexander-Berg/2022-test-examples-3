{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-disk-downloader' %}


{% for file in pillar.get('yandex-disk-downloader-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('yandex-disk-downloader-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('yandex-disk-downloader-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for link, target in pillar.get('yandex-disk-downloader-symlinks').items() %}
{{ link }}:
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}


yandex-disk-downloader:
  pkg:
    - installed
    - pkgs:
      - yandex-disk-downloader-central


