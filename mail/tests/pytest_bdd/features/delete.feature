Feature: Delete messages

  Scenario: Delete message from folder
    Given new initialized user with apple and banana labels
    When we store into "inbox"
      | mid | label       | tid | flags | size | attaches                   |
      | $1  | user:banana | 1   |       | 1    | 1.2:image/jpg:apple.jpg:0  |
      | $2  | user:apple  | 2   | seen  | 2    | 1.2:image/jpg:banana.jpg:0 |
      | $3  | user:apple  | 2   | seen  | 3    | 1.2:image/jpg:tomato.jpg:0 |
      | $4  | user:apple  | 2   | seen  | 4    |                            |
      | $5  | user:banana | 1   |       | 5    |                            |
    Then "inbox" has "5" messages, "2" unseen at revision "8"
    And "user" label "banana" has "2" messages
    And "user" label "apple" has "3" messages
    And in "inbox" there are "2" threads
    And user has "3" messages with attaches, one unseen
    When we delete "$1"
    Then "inbox" has "4" message, "14" size at revision "9"
    And "$1" is deleted
    And "user" label "banana" has "1" messages
    And "user" label "apple" has "3" messages
    And in "inbox" there are "2" threads
      | tid | count | unseen |
      | 1   | 1     | 1      |
      | 2   | 3     | 0      |
    And user has "2" messages with attaches, no unseen
    When we delete "$2, $3, $5"
    Then global revision is "10"
    And "inbox" has one message at revision "10"
    And in "inbox" there is one thread
      | tid | mid | thread_labels |
      | 2   | $4  | user:apple=1  |
    And in "inbox" there is one message
      | mid | label      |
      | $4  | user:apple |
    And "user" label "apple" has one message at revision "10"
    And "user" label "banana" has "0" messages at revision "10"
    And "$2, $3, $5" are deleted
    And user has no messages with attaches
    When we delete "$4"
    Then "$4" is deleted
    And "inbox" is empty at revision "11"
    And "user" label "apple" has "0" messages at revision "11"

  Scenario: Delete last unseen message from folder
    Given new initialized user
    When we store into "inbox"
      | mid | flags |
      | $1  | seen  |
      | $2  |       |
      | $3  | seen  |
    Then in "inbox" there are "3" messages
      | mid | imap_id | flags        |
      | $1  | 1       | seen, recent |
      | $2  | 2       | recent       |
      | $3  | 3       | seen, recent |
    Then "inbox" has "3" messages, first_unseen at "2"
    When we delete "$1, $2"
    Then "$1, $2" are deleted
    And "inbox" has one message, first_unseen at "0"

  Scenario: Delete from 2 folders in one operation
    Given new initialized user
    When we store into "inbox"
      | mid | tid | flags |
      | $1  | 1   | seen  |
      | $2  | 1   | seen  |
      | $3  | 2   |       |
    And we store into "sent"
      | mid | tid | flags |
      | $4  | 1   |       |
      | $5  | 2   | seen  |
      | $6  | 2   | seen  |
    Then in folders "inbox, sent" there are "2" threads
      | tid | message_count | message_unseen |
      | 1   | 3             | 1              |
      | 2   | 3             | 1              |
    And "inbox" has "3" messages, "1" unseen
    And "sent" has "3" messages, "1" unseen
    When we delete "$1, $5"
    Then "$1, $5" are deleted
    And "inbox" has "2" messages, "1" unseen
    And "sent" has "2" messages, "1" unseen
    And in folders "inbox, sent" there are "2" threads
      | tid | message_count | message_unseen |
      | 1   | 2             | 1              |
      | 2   | 2             | 1              |
    When we delete "$2, $6"
    Then "$2, $6" are deleted
    Then in "inbox" there is one thread
      | tid | mid | message_unseen |
      | 2   | $3  | 1              |
    And in "sent" there is one thread
      | tid | mid | message_unseen |
      | 1   | $4  | 1              |
    When we delete "$3, $4"
    Then "$3, $4" are deleted
    And "inbox" is empty
    And "sent" is empty

  Scenario: Delete idempotency
    Given new initialized user with "$1" in "inbox" at revision "2"
    When we delete "$1"
    Then global revision is "3"
    And "inbox" is empty at revision "3"
    And "$1" is deleted
    When we try delete "$1" as "$delete"
    And we commit "$delete"
    Then "$delete" result has unchanged revision
    And global revision is "3"

  Scenario: Delete write changelog
    Given new initialized user with "$[1:4]" in "inbox" at revision "5"
    When we delete "$1, $2, $3"
    And we delete "$4"
    Then in changelog there are
      | revision | type   | mids   |
      | 6        | delete | $[1:3] |
      | 7        | delete | $4     |

  Scenario: Delete write request_info default to null to changlelog
    Given new initialized user with "$1" in "inbox" at revision "2"
    When we delete "$1"
    Then in changelog there are
      | revision | type   | x_request_id | session_key |
      | 3        | delete |              |             |

  Scenario: Delete write request_info to changlelog
    Given new initialized user with "$1" in "inbox" at revision "2"
    When we set request_info "(x-request-id,session-key)"
    And we delete "$1"
    Then in changelog there are
      | revision | type   | x_request_id | session_key |
      | 3        | delete | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Delete write useful_new_count
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we delete "$1"
    Then "delete" is last changelog entry with "1" as useful_new_count

  @changelog @changed
  Scenario: Delete writes valid changed to changelog
    Given new initialized user with "$1" in "inbox"
    When we delete "$1"
    Then last changelog.changed matches "changed/delete_messages.json" schema

  @other-user
  Scenario: Delete do not touch other users
    Given new initialized user with "$1" in "inbox"
    And replication stream
    When we delete "$1"
    Then there are only our user changes in replication stream

  Scenario: Delete messages update tabs
    Given new initialized user
    When we store into tab "news"
      | mid | tid | flags | size |
      | $1  | 1   |       | 1    |
      | $2  | 2   | seen  | 2    |
      | $3  | 2   | seen  | 3    |
      | $4  | 2   | seen  | 4    |
      | $5  | 1   |       | 5    |
    And we delete "$5"
    Then tab "news" has "4" messages, "1" unseen at revision "7"
    And in tab "news" there are "2" threads
      | tid | mid | count | unseen |
      | 1   | $1  | 1     | 1      |
      | 2   | $4  | 3     | 0      |
    And "$5" is deleted
    When we delete "$2, $3, $1"
    Then global revision is "8"
    And tab "news" has one message, no unseen at revision "8"
    And in tab "news" there is "1" thread
      | tid | mid | count | unseen |
      | 2   | $4  | 1     | 0      |
    And "$2, $3, $1" are deleted
    When we delete "$4"
    Then "$4" is deleted
    And tab "news" is empty at revision "9"
