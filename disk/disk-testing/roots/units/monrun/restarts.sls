logger-restarts:
  monrun.present:
    - execution_interval: 300
    - command: /usr/bin/restarts.sh

/usr/bin/restarts.sh:
  file.managed:
    - source: salt://units/monrun/files/usr/bin/restarts.sh
    - mode: 755
    - user: root
    - group: root
    - makedirs: True
