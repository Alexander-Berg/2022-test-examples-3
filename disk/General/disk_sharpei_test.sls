mine_functions:
    grains.item:
        - id
        - role
        - ya
        - pg
        - virtual

{% set shard = salt['grains.get']('id').split('.')[0][-3:-1] %}
{% if shard == '02' %}
{%     set pg_master = salt['grains.get']('id').replace(shard, '01') %}
pg-master: {{ pg_master }}
{% elif shard not in ['01', '02'] %}
{%     set pg_master = salt['grains.get']('id').replace(shard, '02') %}
pg-master: {{ pg_master }}
{% endif %}

data:
    pg:
        version:
            major_num: 1000
    condgroup: disk_sharpei_test
    runlist:
        - components.postgres
        - components.pg-dbs.sharddb
        - components.sharpei
        - components.monrun2.disk
    sharpei:
        cascade: True
        flavor: disk
        zk_hosts: zk1e.dst.yandex.net:2181,zk1f.dst.yandex.net:2181,zk1h.dst.yandex.net:2181
        nginx:
            tskv_format: ydisk-nginx-access-log-sharpei-test
    pgsync:
{% if shard != '01' %}
        replication_source: {{ pg_master }}
{% endif %}
        zk_lockpath_prefix: /pgsync/fsdb-sharpei-test01
    yasmagent:
        prj_split_by_shard: False
        sysconfig_pkg: sharpei
        instances:
            - mailpostgresql
            - mailsharpei
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
        sharpei:
            groups:
                - db_627b6525_e463_4bdc_8147_ef8492976eef
                - db_4e6eb262_e5b2_46b2_a609_ca85654e6b0a
                - db_mdbg613ctnfagpvpigeb
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
    ship_logs: False

gpg-yav-secrets: {{ salt.yav.get('sec-01d3dsca384prk61tvyn37wr0j') | json }}

include:
    - envs.dev
    - disk_test_pgsync_conf
    - private.pg.users.dev.common
    - private.pg.users.dev.disk_sharpei
    - private.pg.users.dev.disk
    - private.abyssync.dev.disk
    - private.pg.tls.dev
    - private.s3.load.disk_test_s3backup
