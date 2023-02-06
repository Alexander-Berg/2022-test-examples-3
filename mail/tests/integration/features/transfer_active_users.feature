Feature: transfer_active_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: transfer_active_users request should work
    When we make transfer_active_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Sets some transfer tasks for active users to default shards
    Given new user "Christian" with "5" messages in "inbox"
    And his change_log is non empty
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make transfer_active_users request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Christian"

  Scenario: Skips non-active users 
    Given new user "Clifford" with "5" messages in "inbox"
    And his change_log is empty
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make transfer_active_users request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no transfer tasks for "Clifford"
