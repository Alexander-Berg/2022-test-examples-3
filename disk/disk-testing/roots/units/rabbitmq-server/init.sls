{% set cluster = pillar.get('cluster') %}
{% set unit = 'rabbitmq-server' %}


{% for file in pillar.get(unit + '-config-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


{% for file in pillar.get(unit + '-exec-files') %}
{{file}}:
  yafile.managed:
    - source: salt://units/{{ unit }}/files{{ file }}
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
{% endfor %}


{% for name,entry in pillar.get(unit + '-dirs', {}).items() %}
{% for dir in entry['dirs'] %}
{{dir}}:
  file.directory:
    - mode: 755 
    - user: rabbitmq 
    - group: rabbitmq
    - makedirs: True
    - require:
        - pkg: rabbitmq-server
{% endfor %}
{% endfor %}




{% for plugin in pillar.get(unit + '-plugins') %}
{{plugin}}:
  rabbitmq_plugin.enabled: []
{% endfor %}

{% for vhost in pillar.get(unit + '-vhosts') %}
{{vhost}}:
  rabbitmq_vhost.present
{% endfor %}

{% for user,settings in pillar.get(unit + '-users').items() %}
{{user}}:
  rabbitmq_user.present:
{#    - password: {{ settings['password'] }} #}
    - tags: {{ settings['tags'] }}
    - perms:
{%-for vhost,permissions in settings['perms'].items() %}
      - {{ vhost }}: {{ permissions }}
{% endfor %}
{% endfor %}


{% for vhost in pillar.get(unit + '-vhosts') %}
rabbitmq-{{vhost}}-alive:
  monrun.present:
    - execution_interval: 60
    - execution_timeout: 10
    - command: /usr/bin/rabbitmq-vhost-alive.py {{vhost}} /etc/monitoring/rabbitmq-vhost-access.yaml
    - type: rabbitmq
{% endfor %}


rabbitmq-server:
  service:
    - running
  pkg:
    - installed

