Feature: Move messages

  Background: All on new initialized user with one message in inbox
    Given new initialized user
    When we store "$1" into tab "relevant"
      | label        | tid | size | attaches                  |
      | system:draft | 42  | 10   | 1.2:image/jpg:apple.jpg:0 |

  Scenario: Move messages sanity check
    Then "inbox" has one message, "10" size at revision "2"
    And global revision is "2"
    And "system" label "draft" has one message at revision "2"
    And user has one message with attaches, not seen

  Scenario: Move from inbox to drafts
    When we move "$1" to "drafts"
    Then "inbox" has not messages at revision "3"
    And "drafts" has one message, one recent, "10" size at revision "3"
    And in "drafts" there is one message
      | mid | tid | imap_id | label        | revision |
      | $1  | 42  | 1       | system:draft | 3        |
    And in "drafts" there is one thread
      | mid | tid | label        | revision | count |
      | $1  | 42  | system:draft | 3        | 1     |
    And "system" label "draft" has one message at revision "2"
    And user has one message with attaches, not seen

  Scenario Outline: Modify flags before move
    When we set "<flags>" on "$1"
    Then "inbox" has one message, "<recent>" recent, "<unseen>" unseen, first_unseen at "<first_unseen_>", at revision "3"
    When we move "$1" to "drafts"
    Then "inbox" has not messages at revision "4"
    And "drafts" has one message, one recent, "<unseen>" unseen, first_unseen at "<first_unseen_>", at revision "4"
    And in "drafts" there is one message
      | mid | tid | revision | flags         |
      | $1  | 42  | 4        | <final_flags> |
    And in "drafts" there is one thread
      | mid | tid | revision | flags         | message_unseen |
      | $1  | 42  | 4        | <final_flags> | <unseen>       |
    Examples:
      | flags         | recent | unseen | first_unseen_ | final_flags  |
      | +seen         | 1      | 0      | 0             | recent,seen  |
      | -recent       | 0      | 1      | 1             | recent       |
      | +seen,-recent | 0      | 0      | 0             | recent, seen |

  Scenario: Move from inbox to trash and back
    When we move "$1" to "trash"
    Then global revision is "3"
    And "inbox" has no messages at revision "3"
    And in "inbox" there are no threads
    And "trash" has one messages one recent at revision "3"
    And in "trash" there is one message
      | mid | tid | label        | revision |
      | $1  |     | system:draft | 3        |
    And "system" label "draft" has "0" messages at revision "3"
    And user has no message with attaches
    When we move "$1" to "inbox"
    Then global revision is "4"
    And "inbox" has one message one recent at revision "4"
    And "trash" has no messages at revision "4"
    And in "inbox" there is one message
      | mid | tid | label        | revision |
      | $1  | 42  | system:draft | 4        |
    And in "inbox" there is one thread
      | mid | tid | label        | revision | count |
      | $1  | 42  | system:draft | 4        | 1     |
    And "system" label "draft" has one message at revision "4"
    And user has one message with attaches, not seen

  Scenario: Move first_unseen message in folder
    When we store "$2" into "inbox"
    And we move "$1" to "trash"
    Then "inbox" has first_unseen at "1", first_unseen_id at "2"
    And "trash" has first_unseen at "1", first_unseen_id at "1"

  Scenario: Move message before first_unseen in folder
    When we store "$2, $3" into "inbox"
    And we set "+seen" on "$1"
    Then "inbox" has first_unseen at "2", first_unseen_id at "2"
    When we move "$1" to "trash"
    Then "inbox" has first_unseen at "1", first_unseen_id at "2"

  Scenario: Move messages after first_unseen
    When we store "$2, $3" into "inbox"
    Then "inbox" has first_unseen at "1", first_unseen_id at "1"
    When we move "$2, $3" to "trash"
    Then "inbox" has first_unseen at "1", first_unseen_id at "1"

  Scenario: Move idempotency
    When we try move "$1" to "inbox" as "$move"
    And we commit "$move"
    Then "$move" result has unchanged revision
    And global revision is "2"

  Scenario: Move write changelog
    When we move "$1" to "trash"
    And we move "$1" to "drafts"
    Then in changelog there are
      | revision | type | x_request_id | session_key |
      | 3        | move |              |             |
      | 4        | move |              |             |

  Scenario: Move write changelog with request_info
    When we set request_info "(x-request-id,session-key)"
    And we move "$1" to "trash"
    Then in changelog there are
      | revision | type | x_request_id | session_key |
      | 3        | move | x-request-id | session-key |

  Scenario Outline: Set doom_date while move
    When we move "$1" to "<folder>"
    Then message "$1" has recent doom_date
    When we move "$1" to "inbox"
    Then message "$1" has null doom_date
    Examples:
      | folder |
      | spam   |
      | trash  |

  @MAILPG-932 @useful_new_count
  Scenario: Update label write useful_new_count to changelog
    When we store "$2" into "inbox"
    And we store "$3" into "trash"
    And we move "$1" to "trash"
    Then "move" is last changelog entry with "1" as useful_new_count

  @changelog @changed
  Scenario: Move writes valid changed to changelog
    When we move "$1" to "trash"
    Then last changelog.changed matches "changed/move_messages.json" schema
    And last changelog.arguments matches "arguments/move_messages.json" schema

  @other-user
  Scenario Outline: Move do not touch other users
    Given replication stream
    When we move "$1" to "<folder>"
    Then there are only our user changes in replication stream
    Examples:
      | folder |
      | drafts |
      | trash  |

  Scenario: Move removes message from tabs thread if dest folder is threaded
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $1  | 42  |
    When we move "$1" to "sent"
    Then in tab "relevant" there are no threads

  Scenario: Move removes message from tabs thread if dest folder is not threaded
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $1  | 42  |
    When we move "$1" to "trash"
    Then in tab "relevant" there are no threads

  Scenario: Should change counter on moving message to nonthreadable folder and back
    When we create "user" label "apple"
    And we accidentally set message_seen to "0" for "user" label "apple"
    And we set "+user:apple" on "$1"
    And we set "+seen" on "$1"
    Then "user" label "apple" has "1" seen messages
    When we move "$1" to "<folder>"
    Then "user" label "apple" has "<count>" seen messages
    When we move "$1" to "inbox"
    Then "user" label "apple" has "1" seen messages
    Examples:
      | folder | count |
      | trash  | 0     |
      | sent   | 1     |
