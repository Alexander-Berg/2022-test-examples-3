{% set unit = 'yandex-selfdns-client' %}

{{ unit }}-files:
  - /etc/cron.d/yandex-selfdns-client
  - /etc/network/projectid
  - /etc/network/interfaces

selfdns_token: {{ salt.yav.get('sec-01cs9cwyav51kgb37hgjgyxbm2[token]') | json }}
