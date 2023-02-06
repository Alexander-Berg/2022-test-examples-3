Feature: /get_user tests

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: 200 must be returned for normal request
    When we request sharpei for /create_user with uid "1" and shard_id "1"
    And we request sharpei for /get_user with uid "1" and mode <mode>
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"

    Examples:
     | mode       |
     | write_only |
     | read_only  |
     | read_write |
     | write_read |
     | all        |


  Scenario: 200 must be returned when force=true
    When we request sharpei for /create_user with uid "2" and shard_id "1"
    And we request sharpei for /get_user with uid "2" and mode <mode> and force is "true"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"

    Examples:
     | mode       |
     | write_only |
     | read_only  |
     | read_write |
     | write_read |
     | all        |


  Scenario: 200 must be returned when force=false
    When we request sharpei for /create_user with uid "3" and shard_id "1"
    And we request sharpei for /get_user with uid "3" and mode <mode> and force is "false"
    Then response status code is "200"
    And response content is "application/json"
    And response is verified by json schema "get_user.json"

    Examples:
     | mode       |
     | write_only |
     | read_only  |
     | read_write |
     | write_read |
     | all        |


  Scenario: uid=(2^63 - 1) must be accepted
    When we request sharpei for /get_user with uid "9223372036854775807" and mode "all"
    Then response status code is "404"
    And response content is "application/json"


  Scenario: 400 must be returned when mode is incorrect
    When we request sharpei for /create_user with uid "4" and shard_id "1"
    And we request sharpei for /get_user with uid "4" and mode "unknown"
    Then response status code is "400"
    And response content is "text/plain"
    And response body contains "failed to parse params: invalid mode value: unknown"


  Scenario: 400 must be returned when mode is missed
    When we request sharpei for /create_user with uid "5" and shard_id "1"
    And we request sharpei for /get_user with uid "5" and without mode
    Then response status code is "400"
    And response content is "text/plain"
    And response body contains "failed to parse params: mode must not be empty"


  Scenario: 404 must be returned when the user doesn't exist
    When we request sharpei for /get_user with uid "6" and mode "all"
    Then response status code is "404"
    And response content is "application/json"
    And response body contains "uid not found"
