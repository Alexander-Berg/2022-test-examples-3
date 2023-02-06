zookeeper:
  wmstesting:
    hosts: mzoo01ht.market.yandex.net:2181,mzoo01et.market.yandex.net:2181,mzoo01vt.market.yandex.net:2181

patroni:
  config:
    scope: wmstesting
    postgresql:
      authentication:
        superuser:
          password: '{{ salt["yav.get"]("sec-01g4d4qwspd814pn2f3xe5mtmd[password]") }}'
        replication:
          password: '{{ salt["yav.get"]("sec-01g4d4qwspd814pn2f3xe5mtmd[password]") }}'
        rewind:
          password: '{{ salt["yav.get"]("sec-01g4d4qwspd814pn2f3xe5mtmd[password]") }}'
      parameters:
        ssl: true
        ssl_cert_file: /etc/patroni/certs/wms-postgres.crt
        ssl_key_file: /etc/patroni/certs/wms-postgres.key
    zookeeper:
      hosts:
        - mzoo01ht.market.yandex.net:2181
        - mzoo01et.market.yandex.net:2181
        - mzoo01vt.market.yandex.net:2181
    bootstrap:
      dcs:
        synchronous_mode: true

postgresql:
  users:
    shippingsorter:
      password: '{{ salt["yav.get"]("sec-01g88g2wk0fge6pncfwtzyfsm0[shippingsorter_password]") }}'
    scheduler:
      password: '{{ salt["yav.get"]("sec-01g88g2wk0fge6pncfwtzyfsm0[scheduler_password]") }}'

  databases:
    shippingsorter:
      owner: shippingsorter
    scheduler:
      owner: scheduler

  schemas:
    sorter:
      dbname: shippingsorter
    scheduler:
      dbname: scheduler

