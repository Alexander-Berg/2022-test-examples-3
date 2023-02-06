include:
    - components.mongodb30

{% set shard = salt['grains.get']('id').split('.')[0][-2:][:-1] %}
/u0/mongodb/doc{{shard}}:
    file.directory:
        - user: mongodb
        - group: mongodb
        - mode: 755
        - makedirs: True
        - require_pre:
            - components.mongodb30

/etc/mongodb.conf:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/doc{{shard}}/mongodb.conf
        - mode: 644

/etc/default/mongodb:
    file.managed:
        - template: jinja
        - source: salt://components/mongodb-dbs/docdb_test/conf/mongodb-default
        - mode: 644
