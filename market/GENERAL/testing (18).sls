zookeeper: # в скором времени это будет стандарт для пиллара зукипера, нужен для репликации
  client_port: 2181
  server_port: 2182
  elect_port: 2183
  nodes:
    1: zkhost01ht.market.yandex.net
    2: zkhost01gt.market.yandex.net
    3: zkhost01dt.market.yandex.net
