{% set unit = 'rabbitmq-server' %}

{{ unit }}-config-files:
  - /etc/rabbitmq/rabbitmq-env.conf
  - /etc/rabbitmq/rabbitmq.config
  - /etc/rabbitmq/enabled_plugins
  - /etc/default/rabbitmq-server


{{ unit }}-exec-files:
  - /usr/bin/rabbitmq-connections-stats.py
  - /usr/bin/rabbitmq-overview-stats.py
  - /usr/bin/rabbitmq-vhost-alive.py
  - /usr/bin/rabbitmq-vhost-stats.py
  - /usr/bin/rabbitmqadmin

