Feature: /get_user tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Preparing database for testing
    Given the first alive host with the role "master" was deleted in shard "3"
    And the first alive host with the role "master" was killed in shard "4"


  Scenario: Master must be returned when mode is write_only
    When we request sharpei for /create_user with uid "7" and shard_id "1"
    And we request sharpei for /get_user with uid "7" and mode "write_only"
    Then response status code is "200"
    And response is verified by json schema "get_user.json"
    And response matches "master" hosts from shard "1"


  Scenario: 500 must be returned when the shard has only replica and mode is write_only
    When we request sharpei for /create_user with uid "8" and shard_id "3"
    And we request sharpei for /get_user with uid "8" and mode "write_only"
    Then response status code is "500"
    And response content is "application/json"
    And response body contains "No entry in cache"


  Scenario: Replica must be returned when mode is read_only
    When we request sharpei for /create_user with uid "9" and shard_id "1"
    And we request sharpei for /get_user with uid "9" and mode "read_only"
    Then response status code is "200"
    And response is verified by json schema "get_user.json"
    And response matches "replica" hosts from shard "1"


  Scenario: 500 must be returned when the shard has only master and mode is read_only
    When we request sharpei for /create_user with uid "10" and shard_id "2"
    And we request sharpei for /get_user with uid "10" and mode "read_only"
    Then response status code is "500"
    And response content is "application/json"
    And response body contains "No entry in cache"


  Scenario: Master-replica-replica must be returned when mode is write_read
    When we request sharpei for /create_user with uid "11" and shard_id "1"
    And we request sharpei for /get_user with uid "11" and mode "write_read"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And /get_user response matches instances in order "master"-"replica"-"replica" from shard "1"


  Scenario: Master must be returned when the shard has only master and mode is write_read
    When we request sharpei for /create_user with uid "12" and shard_id "2"
    And we request sharpei for /get_user with uid "12" and mode "write_read"
    Then response status code is "200"
    And response is verified by json schema "get_user.json"
    And response matches "master" hosts from shard "2"


  Scenario: Replica must be returned when the shard has only replica and mode is write_read
    When we request sharpei for /create_user with uid "13" and shard_id "3"
    And we request sharpei for /get_user with uid "13" and mode "write_read"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And response matches "replica" hosts from shard "3"


  Scenario: Replica-replica-master must be returned when mode is read_write
    When we request sharpei for /create_user with uid "14" and shard_id "1"
    And we request sharpei for /get_user with uid "14" and mode "read_write"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And /get_user response matches instances in order "replica"-"replica"-"master" from shard "1"


  Scenario: Master must be returned when the shard has only master and mode is read_write
    When we request sharpei for /create_user with uid "15" and shard_id "2"
    And we request sharpei for /get_user with uid "15" and mode "read_write"
    Then response status code is "200"
    And response is verified by json schema "get_user.json"
    And response matches "master" hosts from shard "2"


  Scenario: Replica must be returned when the shard has only replica and mode is read_write
    When we request sharpei for /create_user with uid "16" and shard_id "3"
    And we request sharpei for /get_user with uid "16" and mode "read_write"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And response matches "replica" hosts from shard "3"


  Scenario: Three instances must be returned when mode is all
    When we request sharpei for /create_user with uid "17" and shard_id "1"
    And we request sharpei for /get_user with uid "17" and mode "all"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And /get_user response matches shard "1"


  Scenario: Master must be returned when the shard has only master and mode is all
    When we request sharpei for /create_user with uid "18" and shard_id "2"
    And we request sharpei for /get_user with uid "18" and mode "all"
    Then response status code is "200"
    And response is verified by json schema "get_user.json"
    And response matches "master" hosts from shard "2"


  Scenario: Replica must be returned when the shard has only replica and mode is all
    When we request sharpei for /create_user with uid "19" and shard_id "3"
    And we request sharpei for /get_user with uid "19" and mode "all"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And response matches "replica" hosts from shard "3"


  Scenario: Replica must be returned when the shard has only unreachable host and mode is all
    When we request sharpei for /create_user with uid "20" and shard_id "4"
    And we request sharpei for /get_user with uid "20" and mode "all"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"
    And response matches "replica" hosts from shard "4"


  Scenario: 500 must be returned when the sharddb is broken
    When we stop sharddb
    And we request sharpei for /create_user with uid "21" and shard_id "1"
    And we request sharpei for /get_user with uid "21" and mode "all"
    Then response status code is "500"
    And response content is "application/json"
    And response body contains "error in request to meta database"
