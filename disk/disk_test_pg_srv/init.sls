mine_functions:
    grains.item:
        - id
        - role
        - ya

data:
    l3host: True
    network_autoconf: True
    runlist:
        - components.dom0porto
        - components.monrun2.disk
    array_for_sata: /dev/md/3
    monrun2: True
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin

include:
    - envs.dev
    - private.selfdns.realm-disk
    - private.dom0.dom0porto
    - disk_test_pg_srv.{{ salt['grains.get']('id').replace('.', '_') }}
    - private.push-client.prod.disk
