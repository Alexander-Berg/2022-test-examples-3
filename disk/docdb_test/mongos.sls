include:
    - components.mongodb30.mongos

/etc/default/mongos:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongos-default
        - mode: 644
        - require_pre:
            - components.mongodb30.mongos

/etc/mongos.conf:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongos.conf
        - mode: 644
        - require_in:
            - pkg: mongos-30
