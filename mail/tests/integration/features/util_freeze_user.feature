Feature: freeze_users via shiva util method

  Scenario Outline: Call freeze_user for different states
    Given new user "<name>"
    And he is in "<state>" state "<days_ago>" days and has "<notifies>" notifies
    And passport will respond without errors
    When we make util_freeze_user request
    Then shiva responds ok
    And "<name>" is in "frozen" state
    And there are no unexpected requests to passport

  Examples:
      | name     | state    | days_ago | notifies |
      | freeze_1 | active   | 10       | 0        |
      | freeze_2 | inactive | 10       | 0        |
      | freeze_3 | notified | 10       | 0        |
      | freeze_4 | notified | 10       | 1        |
      | freeze_5 | notified | 40       | 1        |
      | freeze_6 | notified | 0        | 2        |
      | freeze_7 | notified | 20       | 2        |
      | freeze_8 | frozen   | 10       | 2        |


  Scenario Outline: Call freeze_user when passport responds different errors
    Given new user "<name>"
    And he is in "notified" state "11" days and has "2" notifies
    And passport will respond with <error> <times> times
    When we make util_freeze_user request
    Then shiva responds server error
    And "<name>" is in "notified" state
    And there are no unexpected requests to passport

  Examples:
      | name        | error               | times |
      | no_freeze_1 | 500                 | 2     |
      | no_freeze_2 | 400                 | 1     |
      | no_freeze_3 | retriable errors    | 2     |
      | no_freeze_4 | nonretriable errors | 1     |
      | no_freeze_5 | illformed response  | 1     |
