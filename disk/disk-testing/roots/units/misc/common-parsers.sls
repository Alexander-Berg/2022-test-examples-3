{% set cluster = pillar.get('cluster') %}
{% set unit = 'common-parsers' %}

{% for file in pillar.get(unit + '-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/misc/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


common-parsers-extra-pkgs:
  pkg.installed:
    - pkgs:
      {% for pkg in pillar.get(unit + '-extra-pkgs') %}
      - {{ pkg }}
      {% endfor %}  

