{% set cluster = pillar.get('cluster') %}


mpfs-queue2:
  service:
    - running
    - reload: False


{% for file in pillar.get('mpfs-queue2-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 644 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


{% for file in pillar.get('mpfs-queue2-exec-files', []) %}
{{file}}:
  yafile.managed:
    - source: salt://units/mpfs/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}



