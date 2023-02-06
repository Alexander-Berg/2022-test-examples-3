{% set unit = 'drweb' %}

{{ unit }}-files:
  - /etc/cron.d/drweb-specific
  - /etc/logrotate.d/drweb

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/drweb-bases-date.conf
  - /etc/monrun/conf.d/drweb-key-date.conf

{{ unit}}-syslog-ng-files:
  - /etc/syslog-ng/conf-available/drweb.conf

{{ unit }}-exec-files:
  - /usr/bin/drweb.check-bases-date.sh
  - /usr/bin/drweb.scanned-stat.py
  - /usr/bin/drweb.watchdog.sh
  - /usr/bin/drweb.check-key-date.sh
  - /usr/lib/yandex-graphite-checks/available/drweb-scanned-stat.sh

{{ unit }}-config-files:
  - /opt/drweb/drweb32.key
  - /etc/drweb/agent.conf
  - /etc/drweb/drweb32.ini

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/drweb-scanned-stat.sh: /usr/lib/yandex-graphite-checks/available/drweb-scanned-stat.sh
  /etc/syslog-ng/conf-enabled/drweb.conf: /etc/syslog-ng/conf-available/drweb.conf

