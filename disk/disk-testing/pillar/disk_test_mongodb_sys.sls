cluster: disk_test_mongodb-sys

include:
  - units.mongodb-mms-monitoring-agent

mongodb:
  rsyncd_secrets: {{ salt.yav.get('sec-01cryxvnn7bkhrpdt181ydbvwy[rsyncd.secrets]') | json }}