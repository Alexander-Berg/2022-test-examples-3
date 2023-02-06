{% set unit = 'mongodb-mms-monitoring-agent' %}


{{ unit }}-config-files:
  - /etc/mongodb-mms/monitoring-agent.config

{{ unit }}-files:
  - /etc/logrotate.d/mongodb-mms-monitoring-agent 

munin-node-config-files:
  {{unit}}:
    basedir: units/{{unit}}/files
    files:
      - /etc/munin/munin-node.conf

munin-node-symlinks:
  {{unit}}:
    symlinks:
      /etc/munin/plugins/cpu : /usr/share/munin/plugins/cpu
      /etc/munin/plugins/iostat : /usr/share/munin/plugins/iostat
      /etc/munin/plugins/iostat_ios : /usr/share/munin/plugins/iostat_ios

