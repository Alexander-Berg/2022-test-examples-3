Feature: purge_chained_log via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: purge_chained_log request should work
    When we make purge_chained_log request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Purge chained_log records after ttl has expired
    Given new user "Stan" with messages in "inbox"
    And she moved messages from "inbox" to "trash" "33" days ago
    When we make purge_chained_log request
    Then shiva responds ok
    And all shiva tasks finished
    And "Stan" chained_log is empty

  Scenario: Do not purge chained_log records before ttl has expired
    Given new user "Kyle" with messages in "inbox"
    And she moved messages from "inbox" to "trash" "11" days ago
    When we make purge_chained_log request
    Then shiva responds ok
    And all shiva tasks finished
    And "Kyle" chained_log is not empty
