mongodb:
  version: '3.0.12-1.8.trusty'
  port: '27017'
  replsetname: 'delivery'
  graphite_prefix: 'one_min.HOST'
  slowop:
    enabled: 'true'
    threshold: '300'
  internal_auth_key: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-auth-key]") }}'
  admin:
    passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-admin-password]") }}'
  # just list all hosts and their ports
  replicaset:
    - deliback01vt.market.yandex.net:27017
    - deliback01ht.market.yandex.net:27017
    - deli01et.market.yandex.net:27017 # arbitr
  # users format:
  #
  # users:
  #   database:
  #     - username:
  #       - passwd: password
  #       - roles: roles json (e.g.: [{ role: 'root', db: 'admin' }] )
  # NB! use SINGLE quotes!
  users:
    admin:
      monitor:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-monitor-password]") }}'
        roles: [ {role: 'clusterMonitor', db: 'admin'} ]
    delivery:
      delivery:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-delivery-password]") }}'
        roles: [ { role: 'readWrite', db: 'delivery' } ]
      devel-ro:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-devel-ro-password]") }}'
        roles: [ { role: 'read', db: 'delivery' } ]
    delivery2:
      devel-ro:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-devel-ro-password]") }}'
        roles: [ { role: 'read', db: 'delivery2' } ]
    delivery-dsm:
      devel-ro:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-devel-ro-password]") }}'
        roles: [ { role: 'read', db: 'delivery-dsm' } ]
    dpd-adapter:
      dpd-adapter:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-dpd-adapter-password]") }}'
        roles: [ { role: 'readWrite', db: 'dpd-adapter' } ]
      devel-ro:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-devel-ro-password]") }}'
        roles: [ { role: 'read', db: 'dpd-adapter' } ]

memcached_cluster:
  - deli01ht.market.yandex.net:11212
  - deli01gt.market.yandex.net:11212

galera_cluster:
  - deliback01ht.market.yandex.net
  - deliback01gt.market.yandex.net

rabbitmq_user:
  name: delivery
  password: {{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-delivery-rabbitmq-password]") }}

zookeeper_servers:
  - "mzoo01vt.market.yandex.net:2181"
  - "mzoo01ht.market.yandex.net:2181"
  - "mzoo01et.market.yandex.net:2181"

