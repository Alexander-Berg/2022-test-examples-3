{% set unit = 'yandex-yasmagent' %}


{{ unit }}-config-files:
  - /etc/default/yasmagent

{{ unit }}-exec-files:
  - /usr/local/bin/yasmagent_disk_getter.py
