include:
    - components.mongodb30.configsrv

/u0/mongodb/configdb:
    file.directory:
        - user: mongodb
        - group: mongodb
        - mode: 755
        - makedirs: True
        - require_in:
            - pkg: mongodb-config-30

/etc/mongodb-config.conf:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongodb-config.conf
        - mode: 644
        - require_in:
            - pkg: mongodb-config-30

/etc/default/mongodb-config:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongodb-config-default
        - mode: 644
        - require_in:
            - pkg: mongodb-config-30
