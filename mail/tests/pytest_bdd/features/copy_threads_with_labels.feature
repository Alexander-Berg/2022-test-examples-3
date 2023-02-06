Feature: Copy should update thread_labels

  Background: All on new initialized user with one message in inbox
    Given new initialized user with apple and banana labels
    When we store into "inbox"
      | mid | tid | labels                  |
      | $1  | 10  | user:apple              |
      | $2  | 10  | user:banana             |
      | $3  | 10  | user:apple, user:banana |
      | $4  | 10  |                         |
      | $5  | 10  | user:apple              |

  Scenario: Sanity check after background
    Then in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    And in "inbox" there are "5" messages
      | mid | tid | labels                  |
      | $1  | 10  | user:apple              |
      | $2  | 10  | user:banana             |
      | $3  | 10  | user:apple, user:banana |
      | $4  | 10  |                         |
      | $5  | 10  | user:apple              |

  Scenario: Copy to trash and back
    When we copy "$[1:5]" to "trash" new messages "$[1:5]c" appears
    Then "trash" has "5" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    When we copy "$[1:5]c" to "inbox"
    Then in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 10    | user:apple=6, user:banana=4 |
    And "user" label "apple" has "6" messages
    And "user" label "banana" has "4" messages

  Scenario: Partial copy to trash and back
    When we copy "$1, $3" to "trash" new messages "$1c, $3c" appears
    Then "trash" has "2" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    When we copy "$2, $4" to "trash" new messages "$2c, $4c" appears
    Then "trash" has "4" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 5     | user:apple=3, user:banana=2 |
    When we copy "$2c" to "inbox" new message "$2cc" appears
    Then "user" label "banana" has "3" messages
    And in "inbox" there are "6" messages
      | mid  | tid | labels                 |
      | $1   | 10  | user:apple             |
      | $2   | 10  | user:banana            |
      | $3   | 10  | user:apple,user:banana |
      | $4   | 10  |                        |
      | $5   | 10  | user:apple             |
      | $2cc | 10  | user:banana            |
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 6     | user:apple=3, user:banana=3 |
    When we copy "$1c, $3c, $4c" to "inbox"
    Then "user" label "apple" has "5" messages
    And "user" label "banana" has "4" messages
    And in "inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 9     | user:apple=5, user:banana=4 |

  Scenario: Copy from different folders
    When we copy "$1, $2" to "sent" new messages "$1c, $2c" appears
    Then "sent" has "2" message
    And in folders "sent, inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 7     | user:apple=4, user:banana=3 |
    When we copy "$3, $4" to "trash" new messages "$3c, $4c" appears
    Then "trash" has "2" messages
    And in folders "sent, inbox" there is one thread
      | tid | count | thread_labels               |
      | 10  | 7     | user:apple=4, user:banana=3 |
