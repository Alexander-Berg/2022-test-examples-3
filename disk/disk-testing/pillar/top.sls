testing:
  '*':
    - units.common-parsers
    - units.atop
    - units.yandex-hbf-agent
    - units.coredumps-cleaner
    - units.yandex-yasmagent
    - units.yandex-disk-packages
    - units.yandex-selfdns-client
  'c:(disk_test_mpfs.*|disk_qa_mpfs_sandbox|disk_test_mpfs-current)$':
    - match: grain_pcre
    - disk_mpfs
  'c:disk_test_webdav$':
    - match: grain_pcre
    - disk_webdav
  'c:disk_test_load_webdav$':
    - match: grain_pcre
    - disk_test_load_webdav
  'c:disk_diskdb_tst$':
    - match: grain_pcre
    - disk_diskdb_tst
  'c:disk_test_uploader$':
    - match: grain_pcre
    - disk_uploader
  'c:disk_test_loaddb':
    - match: grain
    - disk_test_loaddb
  'c:disk_test_load_mongosdb':
    - match: grain
    - disk_test_load_mongosdb
  'c:disk_test_downloader':
    - match: grain
    - disk_downloader
  'c:disk_dev_downloader':
    - match: grain
    - disk_downloader
  'c:disk_test_dataapi$':
    - match: grain_pcre
    - disk_dataapi
  'c:disk_test_zk$':
    - match: grain_pcre
    - disk_zk
  'c:disk_test_cass$':
    - match: grain_pcre
    - disk_cass
  'c:disk_test_mongodb-unit':
    - match: grain
    - disk_test_mongodb_unit
  'c:disk_test_mongodb-sys':
    - match: grain
    - disk_test_mongodb_sys
  'c:disk_test_mongodb3-sys3':
    - match: grain
    - disk_test_mongodb3_sys3
  'c:disk_diskdb_tst':
    - match: grain
    - disk_diskdb_tst
  'c:disk_test_api.*':
    - match: grain_pcre
    - disk_api
  'c:disk_load_api':
    - match: grain
    - disk_api
  'c:disk_test_mpfs':
    - match: grain
    - disk_mpfs
  'c:disk_mpfs_dev':
    - match: grain
    - disk_mpfs_dev
  'c:disk_test_load_mpfs':
    - match: grain
    - disk_test_load_mpfs
  'c:disk_test_ratelimiter':
    - match: grain
    - disk_ratelimiter
  'c:disk_test_db3_unit':
    - match: grain
    - disk_test_db3_unit
  'c:disk_(test|load)_queue':
    - match: grain_pcre
    - disk_queue
