cluster: disk_test_mongodb_unit

include:
  - units.mongodb-mms-monitoring-agent

mongo-checks-lock-on-primary-only: True

mongodb:
  python3: False
  managed-database: True
  rsyncd_secrets: {{ salt.yav.get('sec-01cryxvnn7bkhrpdt181ydbvwy[rsyncd.secrets]') | json }}
