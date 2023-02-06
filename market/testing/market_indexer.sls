confd:
  datasources:
    datasources.conf:
      ks:
        - "/testing/yandex/market-datasources/indexer/"
      src: indexer/datasources.tmpl
      dest: /etc/yandex/market-datasources/datasources.conf
    zookeeper.conf:
      ks:
        - "/testing/yandex/market-datasources/zookeeper/"
      src: indexer/zookeeper.tmpl
      dest: /etc/yandex/market-datasources/zookeeper.conf
