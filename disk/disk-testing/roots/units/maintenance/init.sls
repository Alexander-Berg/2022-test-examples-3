{% set cluster = pillar.get('cluster') %}
{% set unit = 'maintenance' %}

/usr/bin/maintenance:
  yafile.managed:
    - source:
      - salt://files/{{ cluster }}/usr/bin/maintenance
      - salt://units/{{ unit }}/files/usr/bin/maintenance
    - template: jinja
    - user: root
    - group: root
    - mode: 755
    - makedirs: True
