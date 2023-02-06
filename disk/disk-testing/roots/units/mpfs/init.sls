{% set cluster = pillar.get('cluster') %}

include: 
  - templates.mongodb.common

{% for file in pillar.get('mpfs-common-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

#cluster-specific files (configs etc)
{% for file in pillar.get('mpfs-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://files/{{ cluster }}{{ file }}
    - source: salt://units/mpfs/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get('mpfs-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% set mpfs_extra_packages = pillar.get('mpfs-extra-pkgs', []) %}
{% if mpfs_extra_packages %}
mpfs-extra-pkgs:
  pkg.installed:
    - pkgs:
      {% for pkg in mpfs_extra_packages %}
      - {{ pkg }}
      {% endfor %}
{% endif %}

pingunoque:
  service:
    - running
    - reload: False

pymongo-bson-zero-size:
  monrun.present:
    - command: '/usr/lib/yandex/disk/mpfs/pymongo-bson-zero-size.sh /var/log/mpfs/error-tskv.log 1200 50'
    - execution_interval: 120
    - execution_timeout: 30
    - type: mongodb


/etc/yandex/disk-secret-keys.yaml:
  file.managed:
    - contents: {{ pillar['disk-secret-keys'] | json }}
    - mode: 440
    - user: root
    - group: nginx
    - makedirs: True


/etc/yandex/disk-mpfs-token:
  file.managed:
    - contents: {{ pillar['disk-mpfs-token'] | json }}
    - mode: 440 
    - user: root
    - group: nginx
    - makedirs: True


/etc/yandex/mpfs/access_overrides.yaml:
  file.managed:
    - contents: {{ pillar.get('access-overrides-yaml') | json }}
    - mode: 440
    - user: root
    - group: nginx
    - makedirs: True


/etc/nginx/keys/tvm-asymmetric.public:
  file.managed:
    - contents: {{ pillar['tvm-asymmetric-public'] | json }}
    - mode: 440 
    - user: root
    - group: nginx
    - makedirs: True

