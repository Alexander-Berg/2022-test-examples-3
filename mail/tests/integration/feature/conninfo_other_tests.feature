Feature: Conninfo other tests
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    
  Scenario: 400 must be returned when the uid is missing in the request
    When we request sharpei for conninfo without uid and mode "master"
    Then response status code is "400"
    And response body is "failed to parse params: uid must not be empty"


  Scenario: 2^62 is a correct value for uid 
    When we register uid "4611686018427387904" in sharddb shard "1"
    And we register uid "4611686018427387904" in mdb shard "1"
    And we request sharpei for conninfo with uid "4611686018427387904" and mode "all"
    Then response status code is "200"
    And response contains all instances from shard "1"


  Scenario: 400 must be returned when the uid is invalid in the request
    When we request sharpei for conninfo with uid "18611686018427390000" and mode "master"
    Then response status code is "400"
    And response body is "failed to parse params: invalid argument for uid"


  Scenario: 400 must be returned when the mode is missing in the request
    When we request sharpei for conninfo with uid "123" and without mode
    Then response status code is "400"
    And response body is "failed to parse params: mode must not be empty"


  Scenario: 400 must be returned when the mode is incorrect in the request
    When we request sharpei for conninfo with uid "123" and mode "unknown"
    Then response status code is "400"
    And response body is "failed to parse params: invalid mode value: unknown"
