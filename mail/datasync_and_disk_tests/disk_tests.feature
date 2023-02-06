Feature: Disk tests
  Sharpei answer for disk api

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: 400 must be returned when the uid is invalid in the /get_user request
    When we request sharpei for /get_user with uid equals "text" and mode "all"
    Then response status code is "400"
    And response body contains "failed to parse params: invalid argument for uid"


  Scenario: 400 must be returned when the uid is invalid in the /create_user request
    When we request sharpei for /create_user with uid equals "text"
    Then response status code is "400"
    And response body contains "invalid uid parameter value"


  Scenario: 400 must be returned when the uid is invalid in the /update_user request
    When we request sharpei for /update_user with uid equals "text"
    Then response status code is "400"
    And response body contains "invalid uid parameter value"
