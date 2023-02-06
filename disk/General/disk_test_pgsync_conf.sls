data:
    use_pgsync: True
    pgsync:
        zk_hosts: zk1e.dst.yandex.net:2181,zk1f.dst.yandex.net:2181,zk1h.dst.yandex.net:2181
        min_replicas: 1
        failover_checks: 30
        recovery_timeout: 3600
        change_replication_metric: count,time
