Feature: clean callmebackdb via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: clean request for callmebackdb should work
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished

  Scenario Outline: call clean callmebackdb for processed events after default ttl(30 days)
    Given there is event in DB with key "<event_key>"
    And event "<event_key>" run_at "31" days ago
    And event "<event_key>" has status "<status>"
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished
    And event "<event_key>" is absent in callmebackdb

  Examples:
      | event_key                      | status    |
      | processed_after_default_ttl_1  | notified  |
      | processed_after_default_ttl_2  | failed    |
      | processed_after_default_ttl_3  | cancelled |
      | processed_after_default_ttl_4  | rejected  |


  Scenario Outline: call clean callmebackdb for processed events before default ttl(30 days)
    Given there is event in DB with key "<event_key>"
    And event "<event_key>" run_at "29" days ago
    And event "<event_key>" has status "<status>"
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished
    And event "<event_key>" is present in callmebackdb

  Examples:
      | event_key                       | status    |
      | processed_before_default_ttl_1  | notified  |
      | processed_before_default_ttl_2  | failed    |
      | processed_before_default_ttl_3  | cancelled |
      | processed_before_default_ttl_4  | rejected  |


  Scenario: call clean callmebackdb after specified ttl
    Given there is event in DB with key "after_specified_ttl"
    And event "after_specified_ttl" run_at "16" days ago
    And event "after_specified_ttl" has status "notified"
    When we make clean request for callmebackdb with ttl_days "15"
    Then shiva responds ok
    And all shiva tasks finished
    And event "after_specified_ttl" is absent in callmebackdb

  Scenario: call clean callmebackdb before specified ttl
    Given there is event in DB with key "before_specified_ttl"
    And event "before_specified_ttl" run_at "14" days ago
    And event "before_specified_ttl" has status "notified"
    When we make clean request for callmebackdb with ttl_days "15"
    Then shiva responds ok
    And all shiva tasks finished
    And event "before_specified_ttl" is present in callmebackdb

  Scenario: call clean callmebackdb for pending events
    Given there is event in DB with key "pending_event"
    And event "pending_event" run_at "31" days ago
    And event "pending_event" has status "pending"
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished
    And event "pending_event" is present in callmebackdb

  Scenario: call clean callmebackdb for records in change_log after default ttl(30 days)
    Given there is record in change_log for event "outdated_change"
    And change for "outdated_change" was made "31" days ago
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished
    And change for "outdated_change" is absent in change_log

  Scenario: call clean callmebackdb for records in change_log before default ttl(30 days)
    Given there is record in change_log for event "active_change"
    And change for "active_change" was made "29" days ago
    When we make clean request for callmebackdb
    Then shiva responds ok
    And all shiva tasks finished
    And change for "active_change" is present in change_log

  Scenario: call clean callmebackdb for records in change_log after specified ttl
    Given there is record in change_log for event "change_after_specified_ttl"
    And change for "change_after_specified_ttl" was made "16" days ago
    When we make clean request for callmebackdb with ttl_days "15"
    Then shiva responds ok
    And all shiva tasks finished
    And change for "change_after_specified_ttl" is absent in change_log

  Scenario: call clean callmebackdb for records in change_log before specified ttl
    Given there is record in change_log for event "change_before_specified_ttl"
    And change for "change_before_specified_ttl" was made "14" days ago
    When we make clean request for callmebackdb with ttl_days "15"
    Then shiva responds ok
    And all shiva tasks finished
    And change for "change_before_specified_ttl" is present in change_log
