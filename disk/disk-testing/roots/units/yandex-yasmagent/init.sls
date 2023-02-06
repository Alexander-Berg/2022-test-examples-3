{% set unit = 'yandex-yasmagent' %}

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

{% for file in pillar.get(unit + '-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}

yandex-yasmagent:
  pkg:
    - installed
