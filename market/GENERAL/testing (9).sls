clickhouse:
  shards:
    machines_cluster: market_health
    cluster_configs:
    - cluster: market_health
      shard_macros: shard
      shard_user: clickphite
      nodes:
        1:
          - welder01et.market.yandex.net
          - welder01ht.market.yandex.net
        2:
          - welder02et.market.yandex.net
          - welder02ht.market.yandex.net
        3:
          - welder03et.market.yandex.net
          - welder03ht.market.yandex.net
        4:
          - welder04et.market.yandex.net
          - welder04ht.market.yandex.net
    - cluster: market_health_next
      shard_macros: shard
      shard_user: clickphite
      nodes:
        1:
          - welder01et.market.yandex.net
          - welder01ht.market.yandex.net
        2:
          - welder02et.market.yandex.net
          - welder02ht.market.yandex.net
        3:
          - welder03et.market.yandex.net
          - welder03ht.market.yandex.net
        4:
          - welder04et.market.yandex.net
          - welder04ht.market.yandex.net
    - cluster: market_mbologs
      shard_macros: market_mbologs_shard
      shard_user: mbologs
      nodes:
        1:
          - welder02et.market.yandex.net
