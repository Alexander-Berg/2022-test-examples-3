mysql:
  databases:
    - delivery_indexer
  users:
    delidx:
      password: {{ salt["yav.get"]("sec-01fz117rt6xgpvrtp7nwgcrkpc[testing-delidx-password]") }}
      host: "%"
    clustercheck:
      password: {{ salt["yav.get"]("sec-01fz117rt6xgpvrtp7nwgcrkpc[testing-clustercheck-password]") }}
      host: localhost
  grants:
    delidx:
      grant: all privileges
      database: delivery_indexer.*
      user: delidx
      host: "%"
    clustercheck:
      grant: process
      database: "*.*"
      user: clustercheck
      host: localhost

galera:
  cluster:
    - delicalc-idxr01ht.market.yandex.net
  sst:
    user: galera
    password: {{ salt["yav.get"]("sec-01fz117rt6xgpvrtp7nwgcrkpc[testing-galera]") }}
