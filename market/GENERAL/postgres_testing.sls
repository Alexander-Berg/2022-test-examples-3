postgres:
  commands:
    initdb: service postgresql initdb

  lookup:
    pkg_repo: deb [arch=amd64] http://repo.postgrespro.ru/pgpro-9.5/ubuntu trusty main
    pkg_repo_file: /etc/apt/sources.list.d/postgrespro.list
    pkgs_extra:
      - postgrespro-contrib-9.5
      - postgrespro-plpython-9.5
    pg_hba: '/etc/postgresql/9.5/main/pg_hba.conf'

  users:
    robot:
      ensure: present
      password: '{{ salt["yav.get"]("sec-01fz0tawztvayb13x15hay2wkz[testing-robot-password]") }}'
      createdb: False
      createroles: False
      createuser: False
      inherit: True
      replication: False

    repl:
      ensure: present
      password: '{{ salt["yav.get"]("sec-01fz0tawztvayb13x15hay2wkz[testing-repl-password]") }}'
      createdb: False
      createroles: False
      createuser: False
      inherit: True
      replication: True

    marketir:
      ensure: present
      privileges:
        raw_model: select

    amaslak:
      ensure: present
      groups: marketir
    astafurovme:
      ensure: present
      groups: marketir
    ayratgdl:
      ensure: present
      groups: marketir
    galaev:
      ensure: present
      groups: marketir
    evkravtsov:
      ensure: present
      groups: marketir
    pochemuto:
      ensure: present
      groups: marketir
    padme:
      ensure: present
      groups: marketir
    catfish:
      ensure: present
      groups: marketir
    chervotkin:
      ensure: present
      groups: marketir
    york:
      ensure: present
      groups: marketir
    andreevdm:
      ensure: present
      groups: marketir
    mrgrien:
      ensure: present
      groups: marketir
    mkrasnoperov:
      ensure: present
      groups: marketir
    mariakuz:
      ensure: present
      groups: marketir
    tanlit:
      ensure: present
      groups: marketir
    ragvena:
      ensure: present
      groups: marketir
    kotelnikov:
      ensure: present
      groups: marketir

  # This section cover this ACL management of the pg_hba.conf file.
  # <type>, <database>, <user>, [host], <method>
  acls:
    - ['local', 'raw_model', 'robot']
    - ['hostssl', 'raw_model', 'robot', '0.0.0.0/0', 'md5' ]
    - ['hostssl', 'raw_model', 'robot', '::/0', 'md5' ]
    - ['host', 'replication', 'repl', '5.255.217.0/25', 'md5']
    - ['host', 'replication', 'repl', '2a02:6b8:0:1a1e::/64', 'md5']
    - ['hostssl', 'all', 'all', '::/0', 'ldap ldapserver=127.0.0.1 ldapprefix="uid=", ldapsuffix=",ou=people,dc=yandex,dc=net" ldapport=636' ]
    - ['hostssl', 'all', 'all', '0.0.0.0/0', 'ldap ldapserver=127.0.0.1 ldapprefix="uid=", ldapsuffix=",ou=people,dc=yandex,dc=net" ldapport=636' ]

  databases:
    raw_model:
      owner: 'robot'
      user: 'robot'
      template: 'template0'
      lc_ctype: 'ru_RU.UTF8'
      lc_collate: 'ru_RU.UTF8'

  # This section will append your configuration to postgresql.conf.
  postgresconf: |
    listen_addresses = '::, 0.0.0.0'
    wal_level = hot_standby
    hot_standby = on
    max_wal_senders = 4
    ssl_cert_file = '/etc/ssl/market/venddb01ht.market.yandex.net.pem'
    ssl_key_file  = '/etc/ssl/market/venddb01ht.market.yandex.net.key'
    ssl_ca_file   = '/etc/nginx/keys/allCAs.pem'
    ssl_crl_file  = '/etc/nginx/keys/combinedcrl'

