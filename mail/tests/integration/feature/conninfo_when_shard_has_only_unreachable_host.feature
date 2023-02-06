Feature: Conninfo when shard has only unrachable host 
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    And the first alive host with the role "master" was killed


  Scenario: Nothing should be returned when shard has only unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "500"

    Examples:
     | mode       |
     | master     |
     | write_only |
     | replica    |


  Scenario: Nothing should be returned when shard has only unreachable host with force parameter
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode <mode> and force is "true"
    Then response status code is "500"

    Examples:
     | mode       |
     | master     |
     | write_only |
     | replica    |


  Scenario: Dead replica should be returned when shard has only unreachable host with force parameter
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode <mode> and force is "true"
    Then response status code is "200"
    And response json matches expected dead "replica" where shard "1"

    Examples:
     | mode       |
     | read_only  |
     | read_write |
     | write_read |
     | all        |


  Scenario: Dead replica should be returned when shard has only unreachable host
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "200"
    And response json matches expected dead "replica" where shard "1"

    Examples:
     | mode       |
     | read_only  |
     | read_write |
     | write_read |
     | all        |
