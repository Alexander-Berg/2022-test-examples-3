/etc/mongodb.key:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongodb.key
        - mode: 400
        - user: mongodb
