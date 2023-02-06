Feature: Copy many messages with labels

  Scenario: Copy messages from inbox to inbox
    Given new initialized user
    When we create "user" label "apple"
    And we create "user" label "banana"
    Then global revision is "3"
    When we store into "inbox"
      | mid | tid | size | label       | flags | attributes |
      | $1  | 42  | 10   | user:apple  |       | postmaster |
      | $2  | 42  | 20   |             | seen  |            |
      | $3  | 13  | 30   | user:banana |       |            |
    Then global revision is "6"
    And in "inbox" there are "3" messages
      | mid | tid | size | label       | flags        | attributes | imap_id |
      | $1  | 42  | 10   | user:apple  | recent       | postmaster | 1       |
      | $2  | 42  | 20   |             | recent, seen |            | 2       |
      | $3  | 13  | 30   | user:banana | recent       |            | 3       |
    When we set "-recent" on "$2"
    Then global revision is "7"
    And "inbox" has "2" recent at revision "7"
    When we copy "$[1:3]" to "inbox" new messages "$[1:3]c" appears
    Then global revision is "8"
    And "inbox" has "6" messages, "5" recent, "4" unseen, "120" size at revision "8"
    And in "inbox" there are "6" messages
      | mid | tid | revision | flags        | attributes      | imap_id |
      | $1  | 42  | 4        | recent       | postmaster      | 1       |
      | $2  | 42  | 7        | seen         |                 | 2       |
      | $3  | 13  | 6        | recent       |                 | 3       |
      | $1c | 42  | 8        | recent       | postmaster,copy | 4       |
      | $2c | 42  | 8        | recent, seen | copy            | 5       |
      | $3c | 13  | 8        | recent       | copy            | 6       |
    And in "inbox" there are "2" threads
      | tid | count | unseen | thread_revision |
      | 42  | 4     | 2      | 8               |
      | 13  | 2     | 2      | 8               |
    And "user" label "apple" has "2" messages at revision "8"
    And "user" label "banana" has "2" messages at revision "8"
