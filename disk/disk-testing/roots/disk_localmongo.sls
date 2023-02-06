/etc/mongodb.conf:
  file.managed:
    - source: salt://files/disk_localmongo/etc/mongodb.conf
    - mode: 644
    - user: root
    - group: root
    - makedirs: True
    - watch_in:
      - service: mongodb
mongodb:
  service:
    - running

/var/run/mongodb:
  file.directory:
    - mode: 755
    - user: mongodb
    - group: mongodb
    - makedirs: True
    - require_in:
      - service: mongodb
