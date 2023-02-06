Feature: Datasync tests
  Sharpei answer for datasync api

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: 200 must be returned when the text uid in the create_user request
    When we request sharpei for /create_user with uid equals "123_text"
    Then response status code is "200"
    And response is verified by json schema "shard_v3.json"


  Scenario: 404 must be returned when get_user requested with a textual uid
    When we request sharpei for /get_user with uid equals "text" and mode "all"
    Then response status code is "404"
    And response body contains "uid not found"


  Scenario: Master and two replicas must be returned when the text uid in the get_user request
    When we successfully request sharpei for /create_user with uid equals "foo" and shard_id "1"
    And we request sharpei for /get_user with uid equals "foo" and mode "all"
    Then response status code is "200"
    And /get_user response matches shard "1"


  Scenario: Updated shard must be returned when the text uid in the /update_user request
    When we successfully request sharpei for /create_user with uid equals "bar" and shard_id "1"
    And we successfully request sharpei for /get_user with uid equals "bar" and mode "all"
    And we successfully request sharpei for /update_user with uid equals "bar" shard_id "1" new_shard_id "2" and data "[123]"
    And we request sharpei for /get_user with uid equals "bar" and mode "all"
    Then response status code is "200"
    And response shard is "2"
    And response data is ""[123]""
