Feature: purge_transferred_user via shiva util method

  Background: Should have open for registration shards
    Given all shards have load_type "dbaas_hot" and are open for registration

  Scenario: Call util purge_transferred_user
    Given new user "UtilPurgeTransferredUser"
    And he is in "frozen" state "1" days

    When we make util_archive_user request
    Then shiva responds ok
    And "UtilPurgeTransferredUser" is in second shard in sharddb
    And "UtilPurgeTransferredUser" is_here attribute in main shard is false
    And "UtilPurgeTransferredUser" is_here attribute in second shard is true
    And "UtilPurgeTransferredUser" in second shard is in "archived" state
    And "UtilPurgeTransferredUser" in second shard is in "archivation_complete" archivation state

    When we make util_purge_transferred_user request
    Then shiva responds ok
    And "UtilPurgeTransferredUser" has not record in main shard
    And "UtilPurgeTransferredUser" is_here attribute in second shard is true
