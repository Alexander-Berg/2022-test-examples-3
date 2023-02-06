mysql:
  databases:
    - crm
  users:
    crm:
      password: {{ salt["yav.get"]("sec-01fz0zrb085z2f4k4bwewj61xq[int-testing-crm-password]") }}
      host: "%"
    clustercheck:
      password: {{ salt["yav.get"]("sec-01fz0zrb085z2f4k4bwewj61xq[int-testing-clustercheck-password]") }}
      host: localhost
  grants:
    crm:
      grant: all privileges
      database: crm.*
      user: crm
      host: "%"
    clustercheck:
      grant: process
      database: "*.*"
      user: clustercheck
      host: localhost

galera:
  cluster:
    - intcrm01ht.market.yandex.net
    - intcrm01dt.market.yandex.net
    - intcrm01gt.market.yandex.net
  sst:
    user: galera
    password: {{ salt["yav.get"]("sec-01fz0zrb085z2f4k4bwewj61xq[int-testing-galera]") }}

memcached_cluster:
  - intcrm01ht.market.yandex.net
  - intcrm01dt.market.yandex.net
  - intcrm01gt.market.yandex.net
