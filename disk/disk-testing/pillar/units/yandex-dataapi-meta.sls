{% set unit = 'yandex-dataapi-meta' %}

#{{ unit }}-files:
#  - /etc/cron.d/yandex-dataapi-meta

{{ unit }}-config-files:
  - /etc/yandex/dataapi/profile-memcached.properties
    
