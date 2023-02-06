cluster: disk_test_mongodb3-sys3

include:
  - units.mongodb-mms-monitoring-agent

mongodb:
  python3: False
  mongod:
    replSet: "disk_test_mongodb3-sys3"
    storageEngine: "wiredTiger"
  rsyncd_secrets: {{ salt.yav.get('sec-01cryxvnn7bkhrpdt181ydbvwy[rsyncd.secrets]') | json }}