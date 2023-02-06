{% set unit = 'fastcgi-blackbox-authorizer' %}

{{ unit }}-files:
  - /etc/logrotate.d/fastcgi-blackbox-authorizer.conf
  - /etc/fastcgi2/available/fastcgi-blackbox-authorizer.conf

{{ unit }}-monrun-files:
  - /etc/monrun/conf.d/url-checker-update.conf
  - /etc/monrun/conf.d/authorizer-stats.conf

{{ unit }}-syslog-ng-files:
  - /etc/syslog-ng/conf-available/fastcgi-blackbox-authorizer.conf

{{ unit }}-exec-files:
  - /etc/init.d/fastcgi-blackbox-authorizer
  - /usr/bin/disk_downloader.authorizer.pl
  - /usr/lib/yandex-graphite-checks/available/authorizer.sh

{{ unit }}-symlinks:
  /usr/lib/yandex-graphite-checks/enabled/authorizer.sh: /usr/lib/yandex-graphite-checks/available/authorizer.sh
  /etc/syslog-ng/conf-enabled/fastcgi-blackbox-authorizer.conf: /etc/syslog-ng/conf-available/fastcgi-blackbox-authorizer.conf

{{ unit }}-dirs:
  - /var/cache/fastcgi-blackbox-authorizer

