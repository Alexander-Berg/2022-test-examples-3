billing_errors:
  monrun.present:
    - execution_interval: 60
    - execution_timeout: 30
    - command: timetail -t java -n 60 /var/log/mpfs/default-tskv.log | /usr/bin/billing_errors_watcher.py /var/cache/billing_errors_watcher/billing_errors 86400

/usr/bin/billing_errors_watcher.py:
  file.managed:
    - source: salt://units/monrun/files/usr/bin/billing_errors_watcher.py
    - mode: 755
    - user: root
    - group: root
    - makedirs: True

/var/cache/billing_errors_watcher:
  file.directory:
    - user: root
    - group: root
    - makedirs: True
