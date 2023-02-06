mongodb:
  version: '3.0.12-1.7.trusty'
  use_percona: True
  unix_user: 'mongod'
  init_script: 'mongod'
  name_conf: '/etc/mongod.conf'
  port: '27017'
  replsetname: 'stress'
  slowop:
    enabled: 'true'
    threshold: '300'
  internal_auth_key: '{{ salt["yav.get"]("sec-01fz0svy88fhpp63kr4syrxrxz[auth-key]") }}'
  graphite_prefix: 'one_min.HOST'
  admin:
    passwd: '{{ salt["yav.get"]("sec-01fz0svy88fhpp63kr4syrxrxz[admin-passwd]") }}'
  # just list all hosts and their ports
  replicaset:
    - mongostress01dt.market.yandex.net:27017
    - mongostress02dt.market.yandex.net:27017
    - mongostress03dt.market.yandex.net:27017
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
        passwd: '{{ salt["yav.get"]("sec-01fz0svy88fhpp63kr4syrxrxz[monitor-passwd]") }}'
        roles: [ { role: 'monitor', db: 'admin' } ]
    stresstest:
      stresstest:
        passwd: '{{ salt["yav.get"]("sec-01fz0svy88fhpp63kr4syrxrxz[stresstest-passwd]") }}'
        roles: [ { role: 'readWrite', db: 'stresstest' } ]
