{% set unit = 'esets' %}

{{ unit }}-files:
  - /etc/logrotate.d/esets
  - /etc/cron.d/esets
  - /u0/av_tmp/test-av

{{ unit}}-syslog-ng-files:
  - /etc/syslog-ng/conf-available/esets.conf

{{ unit }}-exec-files:
#  - /usr/bin/esets-check-av.sh
  - /usr/bin/esets-watchdog.sh
  - /usr/bin/esets-check-bases-date.sh
  - /usr/bin/esets-check-key-date.sh
  - /usr/bin/esets-check-icap.py

{{ unit }}-symlinks:
  /etc/syslog-ng/conf-enabled/esets.conf: /etc/syslog-ng/conf-available/esets.conf

{{ unit }}-esets-cfg: {{ salt.yav.get('sec-01cs9etk3q8601fmrnrmtadv57[esets.cfg]') | json }}
{{ unit }}-license-files:
  esets_05d8a8.lic: {{ salt.yav.get('sec-01cs9etk3q8601fmrnrmtadv57[esets_05d8a8.lic.b64]') | json }}
  esets_29d8a8.lic: {{ salt.yav.get('sec-01cs9etk3q8601fmrnrmtadv57[esets_29d8a8.lic.b64]') | json }}
  esets_b4d8a8.lic: {{ salt.yav.get('sec-01cs9etk3q8601fmrnrmtadv57[esets_b4d8a8.lic.b64]') | json }}
