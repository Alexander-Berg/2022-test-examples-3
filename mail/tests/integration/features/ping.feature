Feature: Ping york

  Scenario: Request endpoint /ping via http
    Given nothing
    When we request "ping" with no params
    Then response code is 200
    And response has "pong" with value "pong"
