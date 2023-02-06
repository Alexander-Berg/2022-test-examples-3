Feature: /reset_cache and /stat tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Preparing database for testing
    Given the first alive host with the role "master" was deleted in shard "3"
    And the first alive host with the role "master" was killed in shard "4"


  Scenario: 200 must be returned for normal /stat request
    When we request sharpei for /stat
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "stat_v3.json"


  Scenario: Checking the /stat answer with shard_id
    When we request sharpei for /stat with shard_id "2"
    Then response status code is "200"
    And /stat response matches shard "2"


  Scenario: Checking the /stat answer with invalid shard_id
    When we request sharpei for /stat with shard_id "invalid"
    Then response status code is "400"


  Scenario: Checking the /stat answer with unknown shard_id
    When we request sharpei for /stat with shard_id "222"
    Then response status code is "404"


  Scenario: 200 must be returned for normal /reset_cache request
    When we request sharpei for /reset_cache where shard "2"
    Then response status code is "200"
    And response content is "text/plain"
    And response body is "ok"


  Scenario: 200 must be returned for /stat after /reset_cache
    When we successfully request sharpei for /reset_cache where shard "1"
    And we request sharpei for /stat
    Then response status code is "200"
    And /stat response matches all shards


  Scenario: 404 must be returned for /reset_cache when the shard doesn't exist
    When we request sharpei for /reset_cache where shard "5"
    Then response status code is "404"
    And response body contains "shard not found"


  Scenario: 500 must be returned for /reset_cache when the shard has only unreachable host
    When we request sharpei for /reset_cache where shard "4"
    Then response status code is "500"
    And response body contains "reset error: could not reset info for hosts"


  Scenario: 400 must be returned for /reset_cache when the shard is missed
    When we request sharpei for /reset_cache without shard
    Then response status code is "400"
    And response body contains "failed to parse params: shard must not be empty"


  Scenario: 405 must be returned if /reset_cache requested using the GET method
    When we request sharpei for /reset_cache with GET method where shard "1"
    Then response status code is "405"
    And response body contains "MethodNotAllowed"


  Scenario: 400 must be returned for /reset_cache with negative value of shard
    When we request sharpei for /reset_cache where shard "-1"
    Then response status code is "400"
    And response body contains "invalid argument for shard"


  Scenario: 400 must be returned for /reset_cache with too big value of shard
    When we request sharpei for /reset_cache where shard "4294967296"
    Then response status code is "400"
    And response body contains "failed to parse params: invalid argument for shard"


  Scenario: Dead replica must be returned for /stat if all hosts are unreachable
    When we request sharpei for /stat
    Then response status code is "200"
    Then /stat response matches all shards
