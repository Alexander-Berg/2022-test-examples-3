mine_functions:
    grains.item:
        - id
        - role
        - ya
        - pg
        - virtual

data:
    pg:
        version:
            major_num: 1000
    condgroup: disk_sharpei_datasync_test
    runlist:
        - components.postgres
        - components.pg-dbs.sharddb
        - components.sharpei
        - components.monrun2.disk
    sharpei:
        flavor: datasync
        zk_hosts: zk1e.dst.yandex.net:2181,zk1f.dst.yandex.net:2181,zk1h.dst.yandex.net:2181
    pgsync:
        zk_lockpath_prefix: /pgsync/ds-sharpei-test01
    yasmagent:
        sysconfig_pkg: sharpei
    dbname: sharddb
    separate_array_for_xlogs: False
    config:
        shared_preload_libraries: pg_stat_statements,pg_stat_kcache,repl_mon
        shared_buffers: 128MB
        archive_mode: 'on'
        log_min_duration_statement: 1s
        pool_mode: transaction
        wal_log_hints: 'on'
        max_prepared_transactions: 1000
        log_rotate_by_size: True
    sysctl:
        vm.nr_hugepages: 0
    monrun2: True
    monrun:
        load_average_relative:
            warn: 2
            crit: 4
        nginx_499:
            warn: 3000
            crit: 6000
        sharpei:
            groups:
                - db_76a200e6_8215_49d9_a649_2076a1a8c907
                - db_041e1580_f5d8_4738_b5b9_d02f9e6c9526
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin
    use_walg: True
    use_wale: False
    use_barman: False
    s3_bucket: 'disk-backup-pg'
    walg:
        backup_keep: 14
        delta_max_steps: 2
    ship_logs: True

gpg-yav-secrets: {{ salt.yav.get('sec-01d36sqf69ayaqb2e45s54btfy') | json }}

include:
    - disk_test_pgsync_conf
    - envs.dev
    - private.pg.users.dev.common
    - private.pg.users.dev.sharpei
    - private.pg.tls.prod
    - private.abyssync.dev.disk
    - private.s3.load.disk_test_s3backup
    - private.push-client.prod.disk
