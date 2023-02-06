Feature: /ping tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: 200 must be returned for normal /ping request
    When we /ping sharpei
    Then response status code is "200"
    And response content is "text/plain"
    And response body is "pong"


  Scenario: 200 must be returned for /ping when sharddb is broken
    When we stop sharddb
    And we /ping sharpei
    And we start sharddb
    Then response status code is "200"
    And response content is "text/plain"
    And response body is "pong"


  Scenario: 200 must be returned for normal /pingdb request
    When we /pingdb sharpei
    Then response status code is "200"
    And response content is "text/plain"
    And response body is "pong"


  Scenario: 500 must be returned for /pingdb when sharddb is broken
    When we stop sharddb
    And we /pingdb sharpei
    And we start sharddb
    Then response status code is "500"
    And response content is "text/plain"
    And response body contains "PQconnectPoll failed: connection to server at"
