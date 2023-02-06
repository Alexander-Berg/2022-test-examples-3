Feature: freeze_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard
    And surveillance will respond without errors

  Scenario: freeze_users request should work
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call freeze_users after default state_ttl(10 day)
    Given new user "Francis"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond without errors
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Francis" is in "frozen" state

  Scenario: Call freeze_users after default state_ttl(10 day) when passport responds 500
    Given new user "Boris"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with 500 2 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Boris" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users after default state_ttl(10 day) when passport responds 400
    Given new user "Mahmud"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with 400 1 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Mahmud" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users after default state_ttl(10 day) when passport responds retriable errors
    Given new user "Eva"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with retriable errors 2 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Eva" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users after default state_ttl(10 day) when passport responds nonretriable errors
    Given new user "Tanaka"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with nonretriable errors 1 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Tanaka" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users after default state_ttl(10 day) when passport responds illformed response
    Given new user "Wei"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with illformed response 1 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Wei" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users before default state_ttl(10 day)
    Given new user "Fredrick"
    And he is in "notified" state "9" days and has "2" notifies
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Fredrick" is in "notified" state

  Scenario: Call freeze_users for user with an insufficient number of notifies
    Given new user "Frank"
    And he is in "notified" state "11" days and has "1" notifies
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Frank" is in "notified" state

  Scenario: Call freeze_users for active user
    Given new user "Felix"
    And he is in "active" state "11" days
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Felix" is in "active" state

  Scenario: Call freeze_users for user in different shard
    Given new user "FreezeUserInWrongShard" in main shard
    And he is in "notified" state "11" days and has "2" notifies
    And "FreezeUserInWrongShard" is in second shard in sharddb
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "FreezeUserInWrongShard" is in "notified" state with "-6" notifies
