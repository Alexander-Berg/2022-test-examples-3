Feature: Move messages

  Background: All on new initialized user, 6 messages in inbox, sent and drafts
    Given new initialized user
    When we create "user" label "apple"
    And we create "user" label "banana"
    When we store into "inbox"
      | mid | tid | size | label       | flags |
      | $1  | 42  | 10   | user:apple  |       |
      | $2  | 42  | 20   |             | seen  |
      | $3  | 13  | 30   | user:banana |       |
    And we store into "drafts"
      | mid | tid | size | label       | flags         |
      | $4  | 42  | 40   | user:banana |               |
      | $5  | 42  | 50   |             | seen, deleted |
    And we store into "sent"
      | mid | tid | size | flags |
      | $6  | 13  | 60   | seen  |
    And we set "-recent" on "$2, $5, $6"

  Scenario: Move many sanity check
    Then global revision is "10"
    And "inbox" has "3" messages, "2" unseen, "2" recent, "60" size at revision "10"
    And "drafts" has "2" messages, "1" unseen, "1" recent, "90" size at revision "10"
    And "sent" has one message, "0" unseen, "0" recent, "60" size at revision "10"
    And in "inbox" there are "3" messages
      | mid | tid | revision | flags  |
      | $1  | 42  | 4        | recent |
      | $2  | 42  | 10       | seen   |
      | $3  | 13  | 6        | recent |
    And in "drafts" there are "2" messages
      | mid | tid | revision | flags         |
      | $4  | 42  | 7        | recent        |
      | $5  | 42  | 10       | seen, deleted |
    And in "sent" there is one message
      | mid | tid | revision | flags |
      | $6  | 13  | 10       | seen  |
    And in "inbox" there are "2" threads
      | tid | thread_revision | count | unseen |
      | 42  | 10              | 4     | 2      |
      | 13  | 10              | 2     | 1      |
    And in "drafts" there is one thread
      | tid | thread_revision | count | unseen |
      | 42  | 10              | 4     | 2      |
    And in "sent" there is one thread
      | tid | thread_revision | count | unseen |
      | 13  | 10              | 2     | 1      |
    And "user" label "apple" has one message at revision "4"
    And "user" label "banana" has "2" messages at revision "7"

  Scenario: Move messages into sent
    When we move "$[1:6]" to "sent"
    Then global revision is "11"
    And "inbox" has not messages at revision "11"
    And "drafts" has not messages at revision "11"
    And "sent" has "6" messages, "5" recent, "3" unseen, "210" size at revision "11"
    And in "sent" there are "6" messages
      | mid | tid | revision | flags               |
      | $1  | 42  | 11       | recent              |
      | $2  | 42  | 11       | recent,seen         |
      | $4  | 42  | 11       | recent              |
      | $5  | 42  | 11       | recent,seen,deleted |
      | $3  | 13  | 11       | recent              |
      | $6  | 13  | 10       | seen                |
    And in "sent" there are "2" threads
      | tid | count | unseen | thread_revision |
      | 42  | 4     | 2      | 11              |
      | 13  | 2     | 1      | 11              |
    And "user" label "apple" has one message at revision "4"
    And "user" label "banana" has "2" messages at revision "7"

  Scenario: Move messages into trash and then to inbox
    When we move "$1, $2, $5" to "trash"
    Then global revision is "11"
    And "inbox" has one message, one unseen, one recent, "30" size at revision "11"
    And in "inbox" there is one thread
      | tid | count | unseen | thread_revision |
      | 13  | 2     | 1      | 10              |
    And "drafts" has one message, one unseen, one recent, "40" size at revision "11"
    And in "drafts" there is one thread
      | tid | count | unseen | thread_revision |
      | 42  | 1     | 1      | 11              |
    And "trash" has "3" messages, "3" recent, "1" unseen, "80" size at revision "11"
    When we move "$3, $4, $6" to "trash"
    Then global revision is "12"
    And "inbox" has no messages at revision "12"
    And "sent" has no messages at revision "12"
    And "drafts" has no messages at revision "12"
    And "trash" has "6" messages, "6" recent, "3" unseen, "210" size at revision "12"
    And in "trash" there are "6" messages
      | mid | tid | flags               | revision |
      | $1  |     | recent              | 11       |
      | $2  |     | recent,seen         | 11       |
      | $3  |     | recent              | 12       |
      | $4  |     | recent              | 12       |
      | $5  |     | recent,seen,deleted | 11       |
      | $6  |     | recent,seen         | 12       |
    When we move "$[1:6]" to "inbox"
    Then global revision is "13"
    And "trash" has no messages at revision "13"
    And "inbox" has "6" messages, "3" unseen, "6" recent, "210" size at revision "13"
    And in "inbox" there are "6" messages
      | mid | tid | revision | flags               |
      | $1  | 42  | 13       | recent              |
      | $2  | 42  | 13       | recent,seen         |
      | $4  | 42  | 13       | recent              |
      | $5  | 42  | 13       | recent,seen,deleted |
      | $3  | 13  | 13       | recent              |
      | $6  | 13  | 13       | recent,seen         |
    And in "inbox" there are "2" threads
      | tid | count | unseen | thread_revision |
      | 42  | 4     | 2      | 13              |
      | 13  | 2     | 1      | 13              |

