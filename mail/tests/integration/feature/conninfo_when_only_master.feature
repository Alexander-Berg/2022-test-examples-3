Feature: Conninfo when shard has only master
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Master should be returned when shard has only master
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "200"
    And response json matches expected "master" where shard "1"

    Examples:
     | mode       |
     | write_only |
     | read_write |
     | write_read |
     | all        |


  Scenario: Nothing should be returned to the replica request when shard has only master
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "500"

    Examples:
     | mode      |
     | replica   |
     | read_only |


  Scenario: Nothing should be returned to the replica request with force parameter when shard has only master
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode <mode> and force is "true"
    Then response status code is "500"

    Examples:
     | mode      |
     | replica   |
     | read_only |
