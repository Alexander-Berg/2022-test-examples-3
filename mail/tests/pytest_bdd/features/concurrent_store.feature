Feature: Concurrent store message

  Background: New user
    Given new initialized user

  Scenario: Store two messages one into "inbox", second into "drafts"
    When we try store "$1" into "inbox" as "$first"
    And we try store "$2" into "drafts" as "$second"
    When we commit "$first"
    And we commit "$second"
    Then in "inbox" there is one message
    And in "drafts" there is one message

  Scenario Outline: Store two messages in "inbox"
    When we try store "$1" into "inbox" as "$first"
    And we try store "$2" into "inbox" as "$second"
    When we <first_do> "$first"
    Then "$second" should not wait for any lock
    And in "inbox" there are "<first_count>" messages
    When we <second_do> "$second"
    Then in "inbox" there are "<second_count>" messages

    Examples:
      | first_do | first_count | second_do | second_count |
      | commit   | 1           | commit    | 2            |
      | commit   | 1           | rollback  | 1            |
      | rollback | 0           | commit    | 1            |
      | rollback | 0           | rollback  | 0            |

  Scenario: Store two messages in same thread in one folder
    When we try store "$1" into "inbox" as "$first"
      | tid |
      | 1   |
    And we try store "$2" into "inbox" as "$second"
      | tid |
      | 1   |
    When we commit "$first"
    Then in "inbox" there is one message
    And in "inbox" there is one thread
    When we commit "$second"
    Then in "inbox" there are "2" messages
    And in "inbox" there is one thread
