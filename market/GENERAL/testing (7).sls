mongodb:
  version: '3.0.12-1.8.trusty'
  use_percona: True
  unix_user: 'mongod'
  init_script: 'mongod'
  name_conf: '/etc/mongod.conf'
  port: '27017'
  replsetname: 'multiship'
  graphite_prefix: 'one_min.HOST'
  slowop:
    enabled: 'true'
    threshold: '300'
  internal_auth_key: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-auth-key]") }}'
  admin:
    passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-admin-password]") }}'
  # just list all hosts and their ports
  replicaset:
    - mshp-back01dt.market.yandex.net
    - mshp-back01gt.market.yandex.net
    - mshp-back01ht.market.yandex.net
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
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-monitor-password]") }}'
        roles: [ {role: 'clusterMonitor', db: 'admin'} ]
    delivery:
      delivery:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-delivery-password]") }}'
        roles: [ { role: 'readWrite', db: 'delivery' } ]
    delivery2:
      delivery:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-delivery-password]") }}'
        roles: [ { role: 'readWrite', db: 'delivery2' } ]

memcached_cluster:
  - mshp-back01dt.market.yandex.net:11212
  - mshp-back01gt.market.yandex.net:11212
  - mshp-back01ht.market.yandex.net:11212

galera_cluster:
  - mshp-back01dt.market.yandex.net
  - mshp-back01gt.market.yandex.net
  - mshp-back01ht.market.yandex.net

rabbitmq_user:
  name: delivery
  password: {{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-prestable-testing-delivery-rabbitmq-password]") }}

zookeeper_servers:
  - "mzoo01vt.market.yandex.net:2181"
  - "mzoo01ht.yandex.ru:2181"
  - "mzoo01et.market.yandex.net:2181"
