Feature: Conninfo when shard has only unrachable host 
  Sharpei answer for reset

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    And the first alive host with the role "master" was killed

  Scenario: Dead replica should be returned when shard has only unreachable host
    When we request sharpei for v3/stat
    Then response status code is "200"
    And v3/stat response matches all shards
