Feature: transfer_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: transfer_users request should work
    When we make transfer_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Sets some transfer user tasks to default shards
    Given new user "Andy" with "5" messages in "inbox"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make transfer_users request with min_messages_per_user "3" and max_messages_per_user "7"
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Andy"

  Scenario: Skips non-relevant users 
    Given new user "Abby" with "5" messages in "inbox"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make transfer_users request with min_messages_per_user "100" and max_messages_per_user "1000"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no transfer tasks for "Abby"
