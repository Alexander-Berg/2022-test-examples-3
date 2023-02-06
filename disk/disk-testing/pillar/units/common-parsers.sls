{% set unit = 'common-parsers' %}

{{ unit }}-exec-files:
  - /usr/bin/java-pg-stat.py
  - /usr/bin/java-http-stat.py
  - /usr/bin/java-logbroker-stat.py
  - /usr/bin/logbroker-lag.py

{{ unit }}-extra-pkgs:
  - python-psycopg2
  - postgresql-client

