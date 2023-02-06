Feature: update_mailbox_size via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: update_mailbox_size request should work
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Gather statistics for new user
    Given new user "Frederico" with messages in "inbox"
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished
    And "Frederico" has fresh mailbox statistics

  Scenario: Update user statistics after ttl
    Given new user "Carlo" with messages in "inbox"
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished
    Given "Carlo" statistics was updated "31" days ago
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished
    And "Carlo" has fresh mailbox statistics

    Scenario: Skip updating user statistics before ttl
    Given new user "Franchesca" with messages in "inbox"
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished
    Given "Franchesca" statistics was updated "2" days ago
    When we make update_mailbox_size request
    Then shiva responds ok
    And all shiva tasks finished
    And "Franchesca" has not fresh mailbox statistics
