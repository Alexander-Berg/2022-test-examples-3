Feature: Deleted_conninfo tests
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    
  Scenario: Must reinitialize the deleted user in the same shard
    When user with uid "123" is registered in blackbox
    And we register uid "123" in sharddb shard "1"
    And delete the user with uid "123" from sharddb
    And we request sharpei for deleted_conninfo with uid "123" and mode "all"
    Then response status code is "200"
    And deleted_response is equal to conninfo response with uid "123" and mode "all"


  Scenario: Three instances should be returned for mode all
    When we register uid "123" in sharddb shard "1"
    And delete the user with uid "123" from sharddb
    And we request sharpei for deleted_conninfo with uid "123" and mode "all"
    Then response status code is "200"
    And response contains all instances from shard "1"


  Scenario: 404 should be returned when user is not deleted
    When we register uid "123" in sharddb shard "1"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for deleted_conninfo with uid "123" and mode "all"
    Then response status code is "404"
    And response body contains "uid not found"


  Scenario: 404 must be returned when user registered in blackbox
    When user with uid "123" is registered in blackbox
    And we register uid "123" in mdb shard "1"
    And we request sharpei for deleted_conninfo with uid "123" and mode "master"
    Then response status code is "404"
    And response body contains "uid not found"


  Scenario: 404 must be returned when user not registered in blackbox
    When user with uid "123" is not registered in blackbox
    And we register uid "123" in mdb shard "1"
    And we request sharpei for deleted_conninfo with uid "123" and mode "master"
    Then response status code is "404"
    And response body contains "uid not found"


  Scenario: 404 must be returned when not_pg user
    When blackbox returns not pg for user with uid "123"
    And we register uid "123" in mdb shard "1"
    And we request sharpei for deleted_conninfo with uid "123" and mode "master"
    Then response status code is "404"
    And response body contains "uid not found"


  Scenario: 400 must be returned when the uid is missing in the request
    When we request sharpei for deleted_conninfo without uid and mode "all"
    Then response status code is "400"
    And response body contains "failed to parse params: uid must not be empty"


  Scenario: 400 must be returned when the mode is missing in the request
    When we request sharpei for deleted_conninfo with uid "123" and without mode
    Then response status code is "400"
    And response body contains "failed to parse params: mode must not be empty"


  Scenario: 400 must be returned when the mode is incorrect in the request
    When we request sharpei for deleted_conninfo with uid "123" and mode "unknown"
    Then response status code is "400"
    And response body contains "failed to parse params: invalid mode value: unknown"


  Scenario: 400 must be returned when the uid is invalid in the request
    When we request sharpei for deleted_conninfo with uid "18611686018427390000" and mode "all"
    Then response status code is "400"
    And response body contains "failed to parse params: invalid argument for uid"
