Feature: Reset tests when shard has alive master and alive replicas
  Sharpei answer for reset

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Master should be returned for mode master
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for conninfo with uid "123" mode "master" and force is "true"
    And we request sharpei for v3/stat
    Then response status code is "200"
    And v3/stat response matches all shards
