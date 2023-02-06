{% set unit = 'yandex-ipset-persistent' %}

{{ unit }}-files:
  - /etc/ipset/proxy-deny-v4.set
  - /etc/ipset/proxy-deny-v6.set

#{{ unit }}-monrun-files:
#  - /etc/monrun/conf.d/yandex-ipset-persistent.conf

