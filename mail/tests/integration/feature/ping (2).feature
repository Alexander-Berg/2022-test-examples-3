Feature: Ping
  Sharpei answer for ping

  Scenario: Ping
    Given sharpei is started
    When we /ping sharpei
    Then response status code is "200"
    And response body is "pong"

  Scenario: Ping with broken sharddb
    Given sharpei is started
    When we stop sharddb
    And we /ping sharpei
    And we start sharddb
    Then response status code is "200"
    And response body is "pong"

  Scenario: Pingdb
    Given sharpei is started
    When we /pingdb sharpei
    Then response status code is "200"
    And response body is "pong"

  Scenario: Pingdb with broken sharddb
    Given sharpei is started
    When we stop sharddb
    And we /pingdb sharpei
    And we start sharddb
    Then response status code is "500"
