data:
    l3host: True
    runlist:
        - components.monrun2.mongodb
        - components.monrun2.disk
        - components.mongodb30.auth
        - components.mongodb-dbs.docdb_test
        - components.mongodb-dbs.docdb_test.mongos
        - components.mongodb-dbs.docdb_test.configsrv
        - components.hw-watcher
    monrun2: True
    mongodb_version: 3.0.12
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin
    mongodb:
        use_ssl: False
common:
    condgroup: mail_docdb_test

include:
    - envs.dev
    - private.mongodb.dev.docdb
    - private.selfdns.realm-mail
