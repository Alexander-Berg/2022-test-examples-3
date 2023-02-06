Feature: Reset tests when shard has master and unrachable host 
  Sharpei answer for reset

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    And the first alive host with the role "replica" was killed

  Scenario: 500 must be returned when the shard has unreachable host
    When we request sharpei for reset where shard "1"
    Then response status code is "500"
    And response body contains "reset error: could not reset info for hosts:"
