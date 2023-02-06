clickhouse:
  shards: # секция описывает репликацию и шардирование, есть возможность описать несколько кластеров одновременно
    machines_cluster: market_example # "идейное" объединение из машин
    cluster_configs: # https://clickhouse.yandex/reference_ru.html#Distributed
    - cluster: market_example # имя кластера ClickHouse
      shard_macros: shard # переменная, которая появится в replica_macros.xml с номером шарда на текущей ноде, должен быть уникальным в рамках market_example
      shard_user: user1 # пользователь, под которым будут выполняться распределённые запросы
      nodes: # "узлы" шардирования. Если нужна только репликация - указываем только 1 ноду, если только шардирование - по одному хосту на ноду
        1:
          - example01gt.market.yandex.net
          - example01ht.market.yandex.net
        2:
          - example02gt.market.yandex.net
          - example02ht.market.yandex.net
        3:
          - example03gt.market.yandex.net
          - example03ht.market.yandex.net
    - cluster: market_another_example
      shard_macros: another_shard
      shard_user: user2
      nodes:
        1:
          - example01gt.market.yandex.net
          - example01ht.market.yandex.net
          - example02gt.market.yandex.net
        2:
          - example02ht.market.yandex.net
          - example03gt.market.yandex.net
          - example03ht.market.yandex.net
