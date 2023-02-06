cluster: disk_test_db3_unit

include:
  - units.mongodb-mms-monitoring-agent


mongo-checks-lock-on-primary-only: True

disk_test_db3_unit-files:
  - /etc/monitoring/mongodb_indexes/conf.d/unit.json

mongodb:
  python3: False
  managed-database: True
  rsyncd_secrets: {{ salt.yav.get('sec-01cryxvnn7bkhrpdt181ydbvwy[rsyncd.secrets]') | json }}
