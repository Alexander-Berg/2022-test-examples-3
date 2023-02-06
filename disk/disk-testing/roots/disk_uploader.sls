{% set cluster = pillar.get('cluster') %}

include:
  - units.yandex-disk-uploader
  - units.nginx
  - units.nginx.tskv
  - units.statbox-push-client
  # - units.esets
  - templates.certificates
  - templates.mediastorage-mulcagate
  - templates.lepton

{% for file in pillar.get( 'disk_uploader-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get( 'disk_uploader-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('disk_uploader-monrun-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for link, target in pillar.get('disk_uploader-symlinks').items() %}
{{ link }}:
  file.symlink:
    - target: {{ target }}
    - makedirs: True
    - force: True
    - require:
      - yafile: {{ target }}
{% endfor %}

{% for pkg in pillar.get('disk_uploader-additional_pkgs') %}
{{pkg}}:
  pkg:
    - installed
{% endfor %}


/etc/yabs-chkdisk-stop:
  file.absent

