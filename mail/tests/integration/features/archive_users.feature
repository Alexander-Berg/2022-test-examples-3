Feature: archive_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And all shards have load_type "dbaas_hot" and are open for registration

  Scenario: archive_users request should work
    When we make archive_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call archive_users after default state_ttl(190 days)
    Given new user "Baldwin"
    And he is in "frozen" state "191" days
    When we make archive_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Baldwin" is in second shard in sharddb
    And "Baldwin" is_here attribute in main shard is false
    And "Baldwin" is_here attribute in second shard is true
    And "Baldwin" in second shard is in "archived" state
    And "Baldwin" in second shard is in "archivation_complete" archivation state

  Scenario: Call archive_users before default state_ttl(190 days)
    Given new user "Baltasar"
    And he is in "frozen" state "189" days
    When we make archive_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Baltasar" is in main shard in sharddb
    And "Baltasar" is_here attribute in main shard is true
    And "Baltasar" in main shard is in "frozen" state

  Scenario: Call archive_users for active user
    Given new user "Bartholomew"
    And he is in "active" state "191" days
    When we make archive_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Bartholomew" is in main shard in sharddb
    And "Bartholomew" is_here attribute in main shard is true
    And "Bartholomew" in main shard is in "active" state

  Scenario: Call archive_users for user in different shard
    Given new user "Bart" in main shard
    And he is in "frozen" state "191" days
    And "Bart" is in second shard in sharddb
    When we make archive_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Bart" is in second shard in sharddb
    And "Bart" in main shard is in "frozen" state
    And "Bart" in main shard is in "archivation_error" archivation state
