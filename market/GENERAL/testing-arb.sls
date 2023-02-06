mongodb:
  version: '3.0.12-1.8.trusty'
  port: '30000'
  replsetname: 'delivery'
  graphite_prefix: 'one_min.HOST'
  name_conf: /etc/mongod.conf
  unix_user: mongod
  slowop:
    enabled: 'true'
    threshold: '300'
  internal_auth_key: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-arb-auth-key]") }}'
  journal: 'false'
  admin:
    passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-arb-admin-password]") }}'
  # just list all hosts and their ports
  replicaset:
    - deliback01gt.market.yandex.net:27017
    - deliback01ht.market.yandex.net:27017
    - deliwrap01ht.market.yandex.net:30000
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
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-arb-monitor-password]") }}'
        roles: [ {role: 'clusterMonitor', db: 'admin'} ]
    delivery:
      delivery:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-arb-delivery-password]") }}'
        roles: [ { role: 'readWrite', db: 'delivery' } ]
    dpd-adapter:
      dpd-adapter:
        passwd: '{{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[testing-arb-dpd-adapter-password]") }}'
        roles: [ { role: 'readWrite', db: 'dpd-adapter' } ]
