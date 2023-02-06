etcd:
  clusters:
    cross_dc:
      peer_port: 3380
      client_port: 3379
      data_dir: /var/lib/etcd_cross
      peers:
        SAS:
          - etcd01ht.market.yandex.net
          - etcd02ht.market.yandex.net
          - etcd03ht.market.yandex.net

