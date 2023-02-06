
cluster : disk_zk

include:
  - units.yandex-zookeeper-disk

zk_secrets: {{ salt.yav.get('sec-01d94mtzw1s6qhsrh3zzy7mb2h') | json }}
