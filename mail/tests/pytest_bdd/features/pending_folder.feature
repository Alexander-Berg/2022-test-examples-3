Feature: Pending folder aspects

  Background: New user with pending folder
    Given new initialized user
    When we create "pending" folder "pending"

  Scenario: Should not increase fresh counter for pending folder
    When we store "$1" into "inbox"
    When we store "$2" into "pending"
    Then fresh counter is "1" and has revision "3"

  Scenario: Should not increase recent counter for pending folder
    When we store "$1" into "inbox"
    When we store "$2" into "pending"
    Then "inbox" has "1" messages, "1" recent

  Scenario: Should not store messages in thread in pending folder
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
      | $2  | 1   |
    Then in "inbox" there is one thread
      | tid | mid | count |
      | 1   | $2  | 2     |
    When we move "$2" to "pending"
    Then in "inbox" there is one thread
      | tid | mid | count |
      | 1   | $1  | 1     |
