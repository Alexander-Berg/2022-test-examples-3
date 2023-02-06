{% set cluster = pillar.get('cluster') %}
{% set unit = 'nginx' %}


/etc/nginx/conf.d/01-access-tskv.conf:
  yafile.managed:
    - source: salt://units/nginx/files/etc/nginx/conf.d/01-access-tskv.conf
    - template: jinja
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
    - watch_in: 
      - service: nginx

