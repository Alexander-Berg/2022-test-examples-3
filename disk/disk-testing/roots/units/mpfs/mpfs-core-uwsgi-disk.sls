{% set cluster = pillar.get('cluster') %}


mpfs-core-uwsgi-disk:
  service:
    - running
    - reload: False


yasmagent:
  monrun.present:
    - command: '/usr/bin/daemon_check.sh yasmagent'
    - execution_interval: 300 

mpfs-disk:
  monrun.present:
    - command: '/usr/bin/http_check.sh ping 80' 
    - execution_interval: 10


{#
mpfs-queue-disk:
  service:
    - running
    - reload: False

mpfs-queue-index:
  service:
    - running
    - reload: False

mpfs-queue-photoslice:
  service:
    - running
    - reload: False

mpfs-queue-minor:
  service:
    - running
    - reload: False


mpfs-queue-index-lifetime:
  monrun.present:
    - execution_interval: 60
    - execution_timeout: 10
    - command: '/usr/bin/mpfs_queue_lifetime queue-index'
    - type: other


mpfs-old-jobs:
  monrun.present:
    - command: '/usr/lib/yandex/disk/mpfs/check_old_jobs.sh 1 432000 50 21600'
    - execution_interval: 120


mpfs-queue-master-available:
    monrun.present:
    - command: '/usr/lib/yandex/disk/mpfs/mpfs-queue-master-available.sh 60 0' 
    - execution_interval: 60


ensure-local-indexes:
  cmd.run:
    - stateful:
      - test_name: "/usr/bin/mongo --quiet localhost:27018/mpfs ensure_local_indexes.js --eval 'var dry_run=true'"
    - name: "/usr/bin/mongo --quiet localhost:27018/mpfs ensure_local_indexes.js"
    - cwd: /usr/lib/yandex/disk/mpfs
    - require:
      - service: mongodb
      - yafile: /usr/lib/yandex/disk/mpfs/ensure_local_indexes.js
    - require_in:
      - service: mpfs-core-uwsgi-disk
      - service: mpfs-queue-disk
      - service: mpfs-queue-index
      - service: mpfs-queue-photoslice

#}

