{% set unit = 'coredumps-cleaner' %}

{{ unit }}-files:
  - /etc/cron.d/coredumps-cleaner

{{ unit }}-exec-files:
  - /usr/bin/coredumps-cleaner.sh