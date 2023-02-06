{% set unit = 'iptables-persistent' %}

{{ unit }}-files:
  - /etc/iptables/rules.v4
  - /etc/iptables/rules.v6

#{{ unit }}-monrun-files:
#  - /etc/monrun/conf.d/iptables-persistent.conf

