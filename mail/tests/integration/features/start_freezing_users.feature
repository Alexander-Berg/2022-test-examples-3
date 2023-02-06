Feature: start_freezing_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: start_freezing_users request should work
    When we make start_freezing_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call start_freezing_users after default state_ttl(1 day)
    Given new user "Alan"
    And he is in "inactive" state "2" days
    When we make start_freezing_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Alan" is in "notified" state

  Scenario: Call start_freezing_users before default state_ttl(1 day)
    Given new user "Adrian"
    And he is in "inactive" state "0" days
    When we make start_freezing_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Adrian" is in "inactive" state

  Scenario: Call start_freezing_users for active user
    Given new user "Alban"
    And he is in "active" state "2" days
    When we make start_freezing_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Alban" is in "active" state
