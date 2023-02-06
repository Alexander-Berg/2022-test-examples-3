Feature: Reset tests when shard has only master
  Sharpei answer for reset

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: only master must be returned after reset
    When we request sharpei for reset where shard "1"
    And we request sharpei for v3/stat
    Then response status code is "200"
    And v3/stat response matches all shards


  Scenario: 404 must be returned when the shard does not exist
    When we request sharpei for reset where shard "2"
    Then response status code is "404"
    And response body contains "shard not found:"


  Scenario: 400 must be returned when the shard is missing in the request
    When we request sharpei for reset without shard
    Then response status code is "400"
    And response body contains "failed to parse params: shard must not be empty"


  Scenario: 405 must be returned when a post request
    When we request sharpei for reset with GET method where shard "1"
    Then response status code is "405"
    And response body contains "MethodNotAllowed"
