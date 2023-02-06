{% set cluster = pillar.get('cluster') %}
{% set unit = 'yandex-zookeeper-disk' %}

{% for file in pillar.get(unit + '-config-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - template: jinja
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

{% for file in pillar.get(unit + '-monrun-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - watch_in: monrun-regenerate
{% endfor %}

{% for file in pillar.get(unit + '-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

/etc/yandex/zookeeper-disk/jaas.conf:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/etc/yandex/zookeeper-disk/jaas.conf
    - mode: 600 
    - user: disk
    - group: disk
    - makedirs: True
    - template: jinja

yandex-zookeeper-disk:
  service:
    - running
  pkg:
    - installed
