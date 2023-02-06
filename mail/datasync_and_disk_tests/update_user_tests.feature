Feature: /update_user tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Preparing database for testing
    Given the first alive host with the role "master" was deleted in shard "3"
    And the first alive host with the role "master" was killed in shard "4"


  Scenario: Updated data must be returned
    When we successfully request sharpei for /create_user with uid "5" and shard_id "1"
    And we successfully request sharpei for /update_user with uid "5" and data "[678]"
    And we request sharpei for /get_user with uid "5" and mode "all"
    Then response status code is "200"
    And response shard is "1"
    And response data is ""[678]""


  Scenario: Updated data must be returned when shard_id passed
    When we successfully request sharpei for /create_user with uid "6" and shard_id "1"
    And we successfully request sharpei for /update_user with uid "6" shard_id "1" and data "[678]"
    And we request sharpei for /get_user with uid "6" and mode "all"
    Then response status code is "200"
    And response shard is "1"
    And response data is ""[678]""


  Scenario: Updated shard must be returned
    When we successfully request sharpei for /create_user with uid "7" and shard_id "1"
    And we successfully request sharpei for /update_user with uid "7" shard_id "1" and new_shard_id "2"
    And we request sharpei for /get_user with uid "7" and mode "all"
    Then response status code is "200"
    And response shard is "2"


  Scenario: Updated shard and data must be returned
    When we successfully request sharpei for /create_user with uid "8" and shard_id "1"
    And we successfully request sharpei for /update_user with uid "8" shard_id "1" new_shard_id "2" and data "[678]"
    And we request sharpei for /get_user with uid "8" and mode "all"
    Then response status code is "200"
    And response shard is "2"
    And response data is ""[678]""


  Scenario: 404 must be returned when the user doesn't exist
    When we request sharpei for /update_user with uid "9" and data "[678]"
    Then response status code is "404"
    And response content is "application/json"
    And response body contains "uid not found"


  Scenario: 400 must be returned if shard_id doesn't match current user shard id
    When we successfully request sharpei for /create_user with uid "10" and shard_id "1"
    And we request sharpei for /update_user with uid "10" shard_id "3" and data "[678]"
    Then response status code is "400"
    And response body contains "invalid user shard id"
    And response content is "application/json"


  Scenario: 400 must be returned if shard_id doesn't match current user shard id
    When we successfully request sharpei for /create_user with uid "11" and shard_id "1"
    And we request sharpei for /update_user with uid "11" shard_id "3" and new_shard_id "4"
    Then response status code is "400"
    And response body contains "invalid user shard id"
    And response content is "application/json"


  Scenario: 400 must be returned if shard_id doesn't match current user shard id
    When we successfully request sharpei for /create_user with uid "12" and shard_id "1"
    And we request /update_user with uid "12" shard_id "3" new_shard_id "4" and data "[678]"
    Then response status code is "400"
    And response body contains "invalid user shard id"
    And response content is "application/json"


  Scenario: 400 must be returned when shard_id missed and new_shard_id passed
    When we successfully request sharpei for /create_user with uid "13" and shard_id "1"
    And we request sharpei for /update_user with uid "13" new_shard_id "2"
    Then response status code is "400"
    And response body contains "shard_id parameter not found but new_shard_id is present"
    And response content is "application/json"


  Scenario: 400 must be returned when shard_id missed and new_shard_id and data passed
    When we successfully request sharpei for /create_user with uid "14" and shard_id "1"
    And we request /update_user with uid "14" new_shard_id "2" and data "[678]"
    Then response status code is "400"
    And response body contains "shard_id parameter not found but new_shard_id is present"
    And response content is "application/json"


  Scenario: 400 must be returned when the uid is missed
    When we successfully request sharpei for /create_user with uid "15" and shard_id "1"
    And we request sharpei for /update_user without uid
    Then response status code is "400"
    And response content is "application/json"
    And response body contains "uid parameter not found"


  Scenario: 500 must be returned when the sharddb is broken
    When we successfully request sharpei for /create_user with uid "16" and shard_id "1"
    And we stop sharddb
    And we request sharpei for /update_user with uid "16" shard_id "2" new_shard_id "3" and data "[678]"
    Then response status code is "500"
    And response content is "application/json"
    And response body contains "error in request to meta database"
