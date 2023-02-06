{% set cluster = pillar.get('cluster') %}

include:
  - units.cassandra

{% for file in pillar.get( 'disk_cass-files') %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for pkg in pillar.get('disk_cass-additional_pkgs') %}
{{pkg}}:
  pkg:
    - installed
{% endfor %}

/etc/yabs-chkdisk-stop:
  file.absent
    
