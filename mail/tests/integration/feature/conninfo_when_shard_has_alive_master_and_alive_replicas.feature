Feature: Conninfo when shard has alive master and alive replicas
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Master should be returned for mode write_only
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode "write_only"
    Then response status code is "200"
    And response json matches expected "master" where shard "1"


  Scenario: Replica should be returned for mode read_only
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode "read_only"
    Then response status code is "200"
    And response json matches expected "replica" where shard "1"


  Scenario: Three instances in order master-replica-replica should be returned for mode write_read
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode "write_read"
    Then response status code is "200"
    And response contains instances in order "master"-"replica"-"replica" from shard "1"


  Scenario: Three instances in order replica-replica-master should be returned for mode read_write
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode "read_write"
    Then response status code is "200"
    And response contains instances in order "replica"-"replica"-"master" from shard "1"


  Scenario: Three instances should be returned for mode all
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode "all"
    Then response status code is "200"
    And response contains all instances from shard "1"
