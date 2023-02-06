Feature: Reset folder unvisited flag

  Background: New user
    Given new initialized user

  Scenario: Reset folder sanity check
    Then "inbox" has not messages at revision "1"
    And "inbox" is not unvisited

  Scenario: Reset unvisited flag on not unvisited folder
    When we reset unvisited flag for "inbox"
    Then "inbox" is not unvisited
    And global revision is "1"

  Scenario: Reset unvisited flag on unvisited folder
    When we store "$1" into "inbox"
    Then "inbox" is unvisited
    And global revision is "2"
    When we reset unvisited flag for "inbox"
    Then "inbox" is not unvisited
    And global revision is "3"

  Scenario: Reset unvisited flag writes to changelog
    When we store "$1" into "inbox"
    And we reset unvisited flag for "inbox"
    Then "folder-reset-unvisited" is last changelog entry

  @other-user
  Scenario: Reset unvisited does not touch other users
    When we store "$1" into "inbox"
    And we setup replication stream
    When we reset unvisited flag for "inbox"
    Then there are only our user changes in replication stream
