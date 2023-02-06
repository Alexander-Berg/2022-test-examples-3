Feature: /create_user tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: Response must match the JSON schema
    When we request sharpei for /create_user with uid "1"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "shard_v3.json"


  Scenario: 500 must be returned when sharddb is broken
    When we stop sharddb
    And we request sharpei for /create_user with uid "2"
    And we start sharddb
    Then response status code is "500"
    And response content is "application/json"
    And response body contains "error in request to meta database"


  Scenario: 400 must be returned when uid is missed
    When we request sharpei for /create_user without uid
    Then response status code is "400"
    And response content is "application/json"
    And response body contains "uid parameter not found"
