mine_functions:
    grains.item:
        - id
        - role
        - ya
        - pg
        - virtual

data:
    connection_pooler: pgbouncer
    condgroup: disk_test_qdb
    readahead_buffer_bytes:
        md2: 32
    runlist:
        - components.postgres
        - components.pg-dbs.diskqdb
        - components.monrun2.disk
        - components.hw-watcher
        - components.yandex-lazy-trim
    dbname: diskqdb
    update_job_counters_limit: 50000
    update_job_counters_threads: 2
    pg:
        version:
            major_num: 1000
    config:
        min_wal_size: 8GB
        max_wal_size: 256GB
        checkpoint_timeout: 30min
        log_min_duration_statement: 500ms
        autovacuum_analyze_scale_factor: '0.00001'
        default_statistics_target: '1000'
        autovacuum_vacuum_cost_limit: 9000
        autovacuum_vacuum_cost_delay: 5
        lock_timeout: 2s
        pool_mode: transaction
        server_reset_query: 'DISCARD ALL'
        server_reset_query_always: 1
        log_keep_days: 7
        application_name_add_host: 0
        max_client_pool_conn: 8000
        log_rotate_by_size: True
        shared_preload_libraries: pg_stat_statements,pg_stat_kcache,repl_mon
        archive_timeout: 60
    pgbouncer:
        count: 20
        internal_count: 4
        log_connections: 0
        log_disconnections: 0
    sysctl:
        vm.swappiness: 0
        vm.nr_hugepages: 66560
    l3host: True
    network_autoconf: True
    monrun:
        pg_replication_lag:
            crit: 600
        pg_log_errors:
            warn: '500\ 100'
            exclude: '"data was relocated to shard"'
        unispace:
            warn: 80
            crit: 90
        pg_vacuum_failure:
            crit: 100
    monrun2: True
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin
    pgsync:
        change_replication_type: no
    yasmagent:
        prj_split_by_shard: False
    use_wale: False
    use_walg: True
    walg:
        backup_keep: 1
        delta_max_steps: 0
        walg_coroutines: 20
    s3_bucket: 'disk-backup-pg'
    use_barman: False
    ship_logs: False
    diskqdb:
        target: 0020


include:
    - disk_test_qdb.{{ salt['grains.get']('id').split('.')[0][:-1] }}
    - index_repack
    - disk_test_pgsync_conf
    - disk_logrotate_conf
    - envs.dev
    - private.pg.users.dev.common
    - private.pg.users.dev.diskq
    - private.pg.users.dev.disk_lenta_loader
    - private.pg.users.dev.disk_notification_center
    - private.pg.tls.dev
    - private.selfdns.realm-disk
    - private.s3.load.disk_test_pgdb_s3backup
    - private.abyssync.dev.disk
