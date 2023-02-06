data:
    runlist:
        - components.pg-backupcheck
        - components.hw-watcher
        - components.monrun2.disk
    l3host: True
    network_autoconf: True
    sysctl:
        vm.nr_hugepages: 0
    monrun2: True
    hw_watcher:
        mail: disk-admin@yandex-team.ru
        reaction:
          - mail
        initiator: disk-admin
    s3:
        endpoint: "https+path://s3.mdst.yandex.net"
        dblist_endpoint: "https://s3.mdst.yandex.net"

include:
    - envs.dev
    - private.pg.users.dev.common
    - private.pg.tls.dev
    - private.selfdns.realm-disk
    - private.pg.backupcheck.keys_dev
    - private.push-client.prod.disk

exclude:
    - barman-incr

