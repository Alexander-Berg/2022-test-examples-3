Feature: Conninfo blackbox tests
  Sharpei answer for conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping
    
  Scenario: 404 should be returned when user not registered in sharddb and not registered in blackbox
    When user with uid "123" is not registered in blackbox
    And we request sharpei for conninfo with uid "123" and mode <mode>
    Then response status code is "404"
    And response body is ""uid not found: empty uid in blackbox result""

    Examples:
     | mode       |
     | master     |
     | write_only |
     | replica    |
     | read_only  |
     | read_write |
     | write_read |
     | all        |


  Scenario: all instances should be returned when user is not registered in sharddb and is not registered in maildb, but is registered in blackbox
    When user with uid "123" is registered in blackbox
    And we request sharpei for conninfo with uid "123" and mode "all"
    Then response status code is "200"
    And response contains all instances from shard "1"


  Scenario: 400 should be returned for not_pg user
    When blackbox returns not pg for user with uid "1234"
    And we request sharpei for conninfo with uid "1234" and mode "master"
    Then response status code is "400"
    And response body is ""user is not pg: hosts.db_id.2 is not pg in blackbox result: 'not_pg'""


  Scenario: 500 should be returned when user is not registered in sharddb, but is registered in maildb and is registered in blackbox
    When user with uid "12345" is registered in blackbox
    And we register uid "12345" in mdb shard "1"
    And we request sharpei for conninfo with uid "12345" and mode "all"
    Then response status code is "500"
    # see https://st.yandex-team.ru/MAILDLV-4455
    # And response body is ""user already registered: user 12345 already registered in mdb""


  Scenario: 500 should be returned when user is deleted from mdb
    When user with uid "123456" is registered in blackbox
    And we register uid "123456" in mdb shard "1"
    And user with uid "123456" deleted from mdb
    And we request sharpei for conninfo with uid "123456" and mode "all"
    Then response status code is "500"
    And response body is ""shard with alive master not found""
