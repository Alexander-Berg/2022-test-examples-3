mongodb:
  version: '3.0.12-1.8.trusty'
  use_percona: True
  unix_user: 'mongod'
  init_script: 'mongod'
  name_conf: '/etc/mongod.conf'
  port: '27017'
  replsetname: 'cs_mbo-mongodb-testing'
  slowop:
    enabled: 'true'
    threshold: '300'
  internal_auth_key: |
    {{ salt["yav.get"]("sec-01fz10mevfyhy0881pdxkvpp2q[testing-auth-key]") | indent(4) }}
  graphite_prefix: 'one_min.HOST'
  admin:
    passwd: '{{ salt["yav.get"]("sec-01fz10mevfyhy0881pdxkvpp2q[testing-admin-password]") }}'
  # just list all hosts and their ports
  replicaset:
    - mbo-dash01dt.supermarket.yandex.net
    - mbo-dash01gt.supermarket.yandex.net
    - mbo-dash01ht.supermarket.yandex.net
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
        passwd: '{{ salt["yav.get"]("sec-01fz10mevfyhy0881pdxkvpp2q[testing-monitor-password]") }}'
        roles: [ { role: 'monitor', db: 'admin' } ]
    mbo_dashbord:
      mbo_dashboard:
        passwd: '{{ salt["yav.get"]("sec-01fz10mevfyhy0881pdxkvpp2q[testing-mbo_dashbord-password]") }}'
        roles: [ { role: 'readWrite', db: 'mbo_dashboard' } ]
      mbo_human:
        passwd: '{{ salt["yav.get"]("sec-01fz10mevfyhy0881pdxkvpp2q[testing-mbo_human-password]") }}'
        roles: [ { role: 'readWrite', db: 'mbo_dashboard' } ]
