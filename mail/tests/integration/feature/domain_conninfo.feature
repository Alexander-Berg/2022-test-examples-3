Feature: Domain conninfo
  Sharpei provides passport domain conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: New domain conninfo
    When there is domain in blackbox
    And we request sharpei for domain conninfo
    Then response status code is "200"
    And response is verified by json schema "shard_v3.json"

  Scenario: Existing domain conninfo
    When there is domain in sharddb
    And we request sharpei for domain conninfo
    Then response status code is "200"
    And response is verified by json schema "shard_v3.json"

  Scenario: Absent domain conninfo
    When there is no domain in blackbox
    And we request sharpei for domain conninfo
    Then response status code is "404"
    And response is verified by json schema "error.json"
