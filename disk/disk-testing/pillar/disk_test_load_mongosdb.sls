cluster: disk_test_load_mongosdb

mongo-checks-lock-on-primary-only: True


mongodb:
  python3: False
  managed-database: True
  rsyncd_secrets: {{ salt.yav.get('sec-01cryxvnn7bkhrpdt181ydbvwy[rsyncd.secrets]') | json }}
