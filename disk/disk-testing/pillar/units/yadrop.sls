{% set unit = 'yadrop' %}

{{ unit }}-files:
  - /etc/cron.d/wd-yadrop
  - /etc/logrotate.d/yadrop.conf

{{ unit }}-exec-files:
  - /usr/lib/yandex-graphite-checks/available/yadrop-mpfs.pl
  - /usr/lib/yandex-graphite-checks/available/yadrop-auth.py
  - /usr/lib/yandex-graphite-checks/available/yadrop.py
  - /usr/lib/yandex-graphite-checks/available/yadrop-crash.py
  - /usr/lib/monitoring/yadrop_bb_stat.pl
  - /usr/bin/yadrop-watchdog.sh


{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/yadrop-mpfs.pl: /usr/lib/yandex-graphite-checks/available/yadrop-mpfs.pl
  /usr/lib/yandex-graphite-checks/enabled/yadrop-auth.py: /usr/lib/yandex-graphite-checks/available/yadrop-auth.py 
  /usr/lib/yandex-graphite-checks/enabled/yadrop.py:  /usr/lib/yandex-graphite-checks/available/yadrop.py
  /usr/lib/yandex-graphite-checks/enabled/yadrop-crash.py:  /usr/lib/yandex-graphite-checks/available/yadrop-crash.py

