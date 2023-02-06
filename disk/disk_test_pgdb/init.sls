mine_functions:
    grains.item:
        - id
        - role
        - ya
        - pg
        - virtual

data:
    l3host: True
    network_autoconf: True
    add_to_sharpei: False
    connection_pooler: pgbouncer
    runlist:
        - components.postgres
        - components.pg-dbs.diskdb.load
        - components.monrun2.disk
        - components.hw-watcher
    dbname: diskdb
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin
    config:
        min_wal_size: 8GB
        max_wal_size: 128GB
        checkpoint_timeout: 30min
        checkpoint_completion_target: 0.9
        shared_preload_libraries: pg_stat_statements,pg_stat_kcache,repl_mon
        effective_cache_size: 100GB
        lock_timeout: 2s
        log_min_duration_statement: 100ms
        autovacuum_vacuum_cost_limit: 1000
        autovacuum_vacuum_cost_delay: 10
        pool_mode: transaction
        server_reset_query: 'DISCARD ALL'
        server_reset_query_always: 1
        log_keep_days: 7
        application_name_add_host: 0
        max_client_pool_conn: 8001
        log_rotate_by_size: True
        archive_timeout: 60
    pgbouncer:
        count: 8
        internal_count: 2
        log_connections: 0
        log_disconnections: 0
    monrun2: True
    monrun:
        unispace:
            warn: 80
            crit: 90
            repack: 95
    use_walg: True
    use_wale: False
    use_barman: False
    s3_bucket: 'disk-backup-pg'
    walg:
      backup_keep: 14
      compression_method: brotli
    auto_resetup: True
    ship_logs: False

gpg-yav-secrets: {{ salt.yav.get('sec-01ct3gzva7mygpsczsekv44axt') | json }}

include:
    - envs.dev
    - disk_test_pgsync_conf
    - private.selfdns.realm-disk
    - private.pg.users.dev.common
    - private.pg.users.dev.dataapi
    - private.pg.users.dev.disk
    - private.pg.users.dev.disk_mpfs
    - private.pg.users.dev.disk_mworker
    - private.pg.users.dev.disk_pworker
    - private.pg.users.dev.disk_sharpei
    - private.abyssync.dev.disk
    - private.pg.tls.dev
    - private.s3.load.disk_test_pgdb_s3backup
    - index_repack
