{% set cluster = pillar.get('cluster') %}
{% set unit = 'deploy-checks' %}

/usr/bin/mpfs-check-version-after-deploy.py:
  yafile.managed:
    - source: salt://units/{{ unit }}/files/usr/bin/mpfs-check-version-after-deploy.py
    - mode: 755
    - user: root
    - group: root
    - makedirs: True