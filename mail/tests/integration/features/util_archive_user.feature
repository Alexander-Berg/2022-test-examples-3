Feature: archive_users via shiva util method

  Background: Should have open for registration shards
    Given all shards have load_type "dbaas_hot" and are open for registration

  Scenario Outline: Call util archive_user for different(not frozen) states
    Given new user "<name>"
    And he is in "<state>" state "<days_ago>" days
    When we make util_archive_user request
    Then shiva responds bad request
    And "<name>" is in "<state>" state

  Examples:
      | name           | state    | days_ago |
      | util_archive_1 | active   | 100      |
      | util_archive_2 | inactive | 100      |
      | util_archive_3 | notified | 100      |


  Scenario: Call util archive_user for frozen user
    Given new user "UtilArchiveFrozenUser"
    And he is in "frozen" state "1" days
    When we make util_archive_user request
    Then shiva responds ok
    And "UtilArchiveFrozenUser" is in second shard in sharddb
    And "UtilArchiveFrozenUser" is_here attribute in main shard is false
    And "UtilArchiveFrozenUser" is_here attribute in second shard is true
    And "UtilArchiveFrozenUser" in second shard is in "archived" state
    And "UtilArchiveFrozenUser" in second shard is in "archivation_complete" archivation state
