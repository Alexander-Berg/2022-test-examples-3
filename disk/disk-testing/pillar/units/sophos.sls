{% set unit = 'sophos' %}

{{ unit }}-files:
  - /etc/logrotate.d/sophos
  - /etc/cron.d/sophos
  - /u0/savdi_tmp/test-av

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/savdid.conf
  - /etc/monrun/conf.d/sophos-update.conf

{{ unit}}-syslog-ng-files:
  - /etc/syslog-ng/conf-available/sophos.conf

{{ unit }}-exec-files:
  - /etc/init.d/savdid
  - /usr/lib/python2.7/sophos/sssp_xml.py
  - /usr/lib/python2.7/sophos/sssp_file.py
  - /usr/lib/python2.7/sophos/sssp_unix.py
  - /usr/lib/python2.7/sophos/sssp_type.py
  - /usr/lib/python2.7/sophos/sssp_utils.py
  - /usr/lib/python2.7/sophos/sssp_unix_type.py
  - /usr/lib/python2.7/sophos/sssp_name.py
  - /usr/lib/python2.7/sophos/__init__.py
  - /usr/bin/sophos-check-fname.py
  - /usr/bin/savdid-check.sh
  - /usr/bin/savdid-watchdog.sh
  - /usr/bin/sophos-update-check.sh
  - /usr/bin/sophos.scanned-stat.py
  - /usr/lib/yandex-graphite-checks/available/sophos-scanned-stat.sh

{{ unit }}-symlinks:
  /etc/syslog-ng/conf-enabled/sophos.conf: /etc/syslog-ng/conf-available/sophos.conf
  /usr/lib/yandex-graphite-checks/enabled/sophos-scanned-stat.sh: /usr/lib/yandex-graphite-checks/available/sophos-scanned-stat.sh


{{ unit }}-config-files:
  - /etc/savdid.conf

{{ unit }}-dirs:
  - /u0/savdi_tmp

