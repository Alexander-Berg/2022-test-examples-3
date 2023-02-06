Feature: Move should update thread_labels

  Background: Background: All on new initialized user with one message in inbox
    Given new initialized user with apple and banana labels
    When we store into tab "relevant"
      | mid | tid | labels                 |
      | $1  | 10  | user:apple             |
      | $2  | 10  | user:banana            |
      | $3  | 10  | user:apple,user:banana |
      | $4  | 10  |                        |
      | $5  | 10  | user:apple             |
    Then in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |

  Scenario: Move to trash and back
    When we move "$[1:5]" to "trash"
    Then "inbox" is empty
    And "trash" has "5" messages
    And "user" label "apple" has not messages
    And "user" label "banana" has not messages
    When we move "$[1:5]" to "inbox"
    Then in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    And "user" label "apple" has "3" messages
    And "user" label "banana" has "2" messages

  Scenario: Partial move to trash and back
    When we move "$1, $3" to "trash"
    Then "user" label "apple" has one message
    And "user" label "banana" has one message
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 3     | user:apple=1, user:banana=1 |
    When we move "$2, $4" to "trash"
    Then "user" label "apple" has one message
    And "user" label "banana" has not messages
    And in "inbox" there is one thread
      | tid | count | mid | thread_labels |
      | 10  | 1     | $5  | user:apple=1  |
    When we move "$2" to "inbox"
    Then "user" label "apple" has one message
    And "user" label "banana" has one message
    And in "inbox" there is one thread
      | tid | count | mid | thread_labels               |
      | 10  | 2     | $5  | user:apple=1, user:banana=1 |
    When we move "$1, $3, $4" to "inbox"
    Then "user" label "apple" has "3" messages
    And "user" label "banana" has "2" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |

  Scenario: Move from different folders
    When we move "$1, $2" to "sent"
    Then "sent" has "2" message
    And "inbox" has "3" messages
    And in folders "sent, inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    When we move "$3, $4" to "trash"
    Then "trash" has "2" message
    And "inbox" has one message
    And in folders "sent, inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 3     | user:apple=2, user:banana=1 |
    And "user" label "apple" has "2" messages
    And "user" label "banana" has "1" messages
    When we move "$[1:5]" to "inbox"
    Then "user" label "apple" has "3" messages
    And "user" label "banana" has "2" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
