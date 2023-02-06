dns_disk-type-a-records:
  monrun.present:
    - execution_interval: 900
    - execution_timeout: 250
    - command: 'sudo -u monitor /usr/local/bin/dns-lookup-check.sh /etc/monitoring/disk_a_hostnames.conf A'

dns_disk-type-aaaa-records:
  monrun.present:
    - execution_interval: 900
    - execution_timeout: 250
    - command: 'sudo -u monitor /usr/local/bin/dns-lookup-check.sh /etc/monitoring/disk_aaaa_hostnames.conf AAAA'

/etc/monitoring/disk_a_hostnames.conf:
  yafile.managed:
    - source: salt://units/monrun/files/etc/monitoring/disk_a_hostnames.conf
    - mode: 644
    - user: root
    - group: root
    - makedirs: True

/etc/monitoring/disk_aaaa_hostnames.conf:
  yafile.managed:
    - source: salt://units/monrun/files/etc/monitoring/disk_aaaa_hostnames.conf
    - mode: 644
    - user: root
    - group: root
    - makedirs: True

/usr/local/bin/dns-lookup-check.sh:
  file.managed:
    - source: salt://units/monrun/files/usr/local/bin/dns-lookup-check.sh
    - mode: 755 
    - user: root
    - group: root
    - makedirs: True
