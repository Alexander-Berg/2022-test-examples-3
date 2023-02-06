{% set unit = 'yandex-hbf-agent' %}

{{ unit }}-config-files:
  - /etc/yandex-hbf-agent/rules.d/10-allow-pingunoque.v4
  - /etc/yandex-hbf-agent/rules.d/10-allow-pingunoque.v6
  - /etc/syslog-ng/conf.d/01-hbf.conf
  - /etc/logrotate.d/yandex-hbf-agent.conf

{{ unit }}-exec-files:
  - /etc/init.d/yandex-hbf-agent
