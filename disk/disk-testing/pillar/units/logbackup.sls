logbackup-files:
  - /etc/disk-logbackup.blacklist

logbackup-secret-files:
  - /usr/lib/yandex/disk/logbackup/rsync.password

logbackup-exec-files:
  - /etc/cron.d/disk-logbackup
  - /usr/lib/yandex/disk/logbackup/check-logbackup.sh
  - /usr/lib/yandex/disk/logbackup/disk-logbackup.sh

logbackup-dirs:
  - /var/log/disk-logs
