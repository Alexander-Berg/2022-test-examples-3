{% set unit = 'memcached' %}

{{ unit }}-files:
  - /etc/memcached.conf

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/memcached.conf

