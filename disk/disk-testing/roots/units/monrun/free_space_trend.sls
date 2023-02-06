{% set config = pillar.get('free_space_trend', { 'execution_interval' : 60 , 'check_interval' : 300, 'prediction_intervals': 24, 'min_space_perc': 5 }) -%}

free_space_trend:
  monrun.present:
    - execution_interval: {{ config['execution_interval'] }}
    - execution_timeout: 15
    - command: /usr/bin/free_space_trend.sh {{ config['check_interval'] }} {{ config['prediction_intervals'] }} {{ config['min_space_perc'] }}

/usr/bin/free_space_trend.sh:
  file.managed:
    - source: salt://units/monrun/files/usr/bin/free_space_trend.sh
    - mode: 755
    - user: root
    - group: root
    - makedirs: True


