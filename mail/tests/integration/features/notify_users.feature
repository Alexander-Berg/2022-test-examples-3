Feature: notify_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And surveillance will respond without errors

  Scenario: notify_users request should work
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call notify_users for users without notifies after default state_ttl(1 day)
    Given new user "Garey"
    And he is in "notified" state "2" days and has "0" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Garey" is in "notified" state with "1" notifies

  Scenario: Call notify_users for users without notifies before default state_ttl(1 day)
    Given new user "Gabriel"
    And he is in "notified" state "0" days and has "0" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gabriel" is in "notified" state with "0" notifies

  Scenario: Call notify_users for users with 1 notify after default state_ttl(20 day)
    Given new user "Gedeon"
    And he is in "notified" state "21" days and has "1" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gedeon" is in "notified" state with "2" notifies

  Scenario: Call notify_users for users with 1 notify before default state_ttl(20 day)
    Given new user "Geoffrey"
    And he is in "notified" state "19" days and has "1" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Geoffrey" is in "notified" state with "1" notifies

  Scenario: Call notify_users for user with too much number of notifies
    Given new user "Gerald"
    And he is in "notified" state "100" days and has "2" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gerald" is in "notified" state with "2" notifies

  Scenario: Call notify_users for active user
    Given new user "Gervais"
    And he is in "active" state "20" days
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gervais" is in "active" state

  Scenario: Call notify_users for user absent in blackbox
    Given new user "Gideon" with uid "11111111"
    And he is absent in blackbox
    And he is in "notified" state "21" days and has "1" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gideon" is in "notified" state with "-2" notifies

  Scenario: Call notify_users for user with Direct subscription in blackbox
    Given new user "Gordon"
    And he has sids "14" in blackbox
    And he is in "notified" state "2" days and has "0" notifies
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Gordon" is in "notified" state with "-4" notifies

  Scenario: Call notify_users for user in different shard
    Given new user "NotifyUserInWrongShard" in main shard
    And he is in "notified" state "21" days and has "1" notifies
    And "NotifyUserInWrongShard" is in second shard in sharddb
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "NotifyUserInWrongShard" is in "notified" state with "-6" notifies
