Feature: Conninfo when shard has master, replica and unrachable host 
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    And the first alive host with the role "replica" was killed


  Scenario: Alive and dead replica should be returned when shard has master, replica and unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "read_only" and force is "true"
    Then response status code is "200"
    And response json matches expected "replica" where shard "1"
    And response json matches expected dead "replica" where shard "1"


  Scenario: Alive master should be returned when shard has master, replica and unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "write_only" and force is "true"
    Then response status code is "200"
    And response json matches expected "master" where shard "1"


  Scenario: Alive master, replica and dead replica should be returned when shard has master, replica and unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "all" and force is "true" 
    Then response status code is "200"
    And response json matches expected "master" where shard "1"
    And response json matches expected "replica" where shard "1"
    And response json matches expected dead "replica" where shard "1"


  Scenario: Alive master, replica and dead replica in order "master-replica-replica" with statuses "alive-alive-dead" should be returned when shard has master, replica and unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "write_read" and force is "true" 
    Then response status code is "200"
    And response json matches expected "master" where shard "1"
    And response json matches expected "replica" where shard "1"
    And response json matches expected dead "replica" where shard "1"
    And response contains instances in order "master-replica-replica" with statuses "alive-alive-dead"


  Scenario: Alive master, replica and dead replica in order "replcia-replica-master" with statuses "alive-alive-dead" should be returned when shard has master, replica and unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "read_write" and force is "true" 
    Then response status code is "200"
    And response json matches expected "master" where shard "1"
    And response json matches expected "replica" where shard "1"
    And response json matches expected dead "replica" where shard "1"
    And response contains instances in order "replica-replica-master" with statuses "alive-alive-dead"
