Feature: Conninfo when two shard has two masters
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    And two hosts in the shard "1" identify themselves as masters

  Scenario: Master should be returned when first shard has only master
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    When we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "200"
    And response json matches expected "master" where shard "1"
    And response contains instances in order "master-master" with statuses "alive-alive"

    Examples:
     | mode       |
     | read_write |
     | write_read |
     | all        |
