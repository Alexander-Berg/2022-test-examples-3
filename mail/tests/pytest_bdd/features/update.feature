Feature: Update messages status

  Scenario Outline: Update one message in folder with one message
    Given new initialized user
    When we store into "inbox"
      | mid | flags         | attaches                  |
      | $1  | <store_flags> | 1.2:image/jpg:apple.jpg:0 |
    And we set "<set_flags>" on "$1"
    Then "inbox" has one message, "<unseen>" unseen, "<recent>" recent at revision "3"
    And in "inbox" there is one message
      | mid | revision | flags          |
      | $1  | 3        | <result_flags> |
    And in "inbox" there is one thread
      | tid | count | unseen   |
      | $1  | 1     | <unseen> |
    And user has one message with attaches, "<unseen>" unseen
    Examples:
      | store_flags | set_flags                | unseen | recent | result_flags          |
      |             | +seen                    | 0      | 1      | seen, recent          |
      |             | +seen, -recent           | 0      | 0      | seen                  |
      |             | +deleted                 | 1      | 1      | deleted, recent       |
      |             | +deleted, -recent, +seen | 0      | 0      | deleted, seen         |
      | seen        | -seen                    | 1      | 1      | recent                |
      | seen        | -seen, -recent           | 1      | 0      |                       |
      | seen        | +deleted                 | 0      | 1      | deleted, recent, seen |
      | seen        | +deleted, -recent, -seen | 1      | 0      | deleted               |

  Scenario: Update idempotency
    Given new initialized user with "$1" in "inbox"
    Then "inbox" has one message, one unseen, one recent at revision "2"
    Then "inbox" has one unseen, one message, one recent at revision "2"
    When we set "+seen" on "$1"
    Then "inbox" has one message, "0" unseen, "1" recent at revision "3"
    When we set "+seen" on "$1"
    Then "inbox" has one message, "0" unseen, "1" recent at revision "3"
    When we set "+deleted" on "$1"
    Then "inbox" has one message, "0" unseen, "1" recent at revision "4"

  Scenario: Update first_unseen in folder with one message
    Given new initialized user with "$42" in "inbox"
    Then "inbox" has first_unseen at "1"
    When we set "+seen" on "$42"
    Then "inbox" has first_unseen at "0"
    When we set "-seen" on "$42"
    Then "inbox" has first_unseen at "1"

  Scenario: Update first_unseen, to next unseen in folder
    Given new initialized user
    When we store into "inbox"
      | mid | flags |
      | $10 |       |
      | $11 | seen  |
      | $12 |       |
    Then "inbox" has first_unseen at "1"
    When we set "+seen" on "$10"
    Then "inbox" has first_unseen at "3"
    When we set "-seen" on "$11"
    Then "inbox" has first_unseen at "2"
    When we set "+seen" on "$12"
    Then "inbox" has first_unseen at "2"
    When we set "+seen" on "$11"
    Then "inbox" has first_unseen at "0"

  Scenario: Update in Spam
    Given new initialized user with "$1" in "spam"
    Then "spam" has one message, one unseen
    When we set "+seen" on "$1"
    Then "spam" has one message, no unseen

  Scenario: Add one label
    Given new initialized user
    When we store "$1" into "inbox"
      | tid |
      | 1   |
    Then "inbox" has one message at revision "2"
    When we create "user" label "apple"
    And we set "+user:apple" on "$1"
    Then "inbox" has one message at revision "4"
    And in "inbox" there is one message
      | mid | label      |
      | $1  | user:apple |
    And in "inbox" there is one thread
      | tid | thread_label |
      | 1   | user:apple=1 |
    And "user" label "apple" has one message at revision "4"

  Scenario: Add 2 labels on 2 message in with same thread
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 42  |
      | $2  | 42  |
    Then "inbox" has "2" messages at revision "3"
    And in "inbox" there is one thread
      | tid | count | revision |
      | 42  | 2     | 3        |
    When we create "user" label "on_both"
    And we create "user" label "on_second_only"
    When we set "+user:on_both" on "$1, $2"
    Then "inbox" has "2" messages at revision "6"
    And in "inbox" there are "2" messages
      | mid | label        | revision |
      | $1  | user:on_both | 6        |
      | $2  | user:on_both | 6        |
    And in "inbox" there is one thread
      | tid | count | thread_label   | revision |
      | 42  | 2     | user:on_both=2 | 6        |
    When we set "+user:on_second_only" on "$2"
    Then "inbox" has "2" messages at revision "7"
    And in "inbox" there are "2" messages
      | mid | labels                            | revision |
      | $1  | user:on_both                      | 6        |
      | $2  | user:on_both, user:on_second_only | 7        |
    And "user" label "on_both" has "2" messages at revision "6"
    And "user" label "on_second_only" has one message at revision "7"
    And in "inbox" there is one thread
      | tid | count | thread_labels                         | revision |
      | 42  | 2     | user:on_both=2, user:on_second_only=1 | 7        |

  Scenario: Remove labels from message
    Given new initialized user
    When we create "user" label "on_both"
    And we create "user" label "on_second_only"
    And we store into "inbox"
      | mid | tid | label                             |
      | $1  | 42  | user:on_both                      |
      | $2  | 42  | user:on_both, user:on_second_only |
    Then in "inbox" there are "2" messages
      | mid | tid | label                             | revision |
      | $1  | 42  | user:on_both                      | 4        |
      | $2  | 42  | user:on_both, user:on_second_only | 5        |
    And in "inbox" there is one thread
      | tid | count | thread_labels                         | revision |
      | 42  | 2     | user:on_both=2, user:on_second_only=1 | 5        |
    And "user" label "on_both" has "2" messages
    And "user" label "on_second_only" has one message
    When we set "-user:on_second_only" on "$1, $2"
    Then "inbox" has "2" messages at revision "6"
    And in "inbox" there are "2" messages
      | mid | label        | revision |
      | $1  | user:on_both | 4        |
      | $2  | user:on_both | 6        |
    And "user" label "on_second_only" has "0" messages
    And in "inbox" there is one thread
      | tid | count | thread_label   | revision |
      | 42  | 2     | user:on_both=2 | 6        |
    When we set "-user:on_both" on "$1, $2"
    Then "inbox" has "2" messages at revision "7"
    And in "inbox" there are "2" messages
      | mid | label | revision |
      | $1  |       | 7        |
      | $2  |       | 7        |
    And in "inbox" there is one thread
      | tid | count | label | revision |
      | 42  | 2     |       | 7        |
    And "user" label "on_both" has not messages

  Scenario: Try capture where we get duplicate lids
    Given new initialized user
    When we store "$1, $2" into "inbox"
      | tid |
      | 42  |
    And we create "user" label "one"
    And we create "user" label "two"
    And we set "+user:two" on "$2"
    And we set "+user:one" on "$1"
    And we set "+user:one, +user:two" on "$1, $2"
    Then "user" label "one" has "2" messages
    And "user" label "two" has "2" messages
    And in "inbox" there are "2" messages
      | mid | labels             |
      | $1  | user:one, user:two |
      | $2  | user:one, user:two |
    And in "inbox" there is one thread
      | tid | thread_labels          |
      | 42  | user:one=2, user:two=2 |
    When we set "-user:one" on "$2"
    And we set "-user:two" on "$1"
    And we set "-user:one, -user:two" on "$1, $2"
    Then "user" label "one" has "0" messages
    And "user" label "two" has "0" messages

  Scenario: Update write to changelog
    Given new initialized user with "$[1:4]" in "inbox"
    Then global revision is "5"
    When we set "+seen" on "$1"
    And we set "+seen" on "$[1:3]"
    And we set "+deleted, -seen" on "$[2:4]"
    Then in changelog there are
      | revision | type   | mids   |
      | 6        | update | $1     |
      | 7        | update | $2, $3 |
      | 8        | update | $[2:4] |

  Scenario Outline: [MAILPG-440] Update messages in <folder>
    Given new initialized user with "$1" in "<folder>"
    When we set "+system:pinned" on "$1"
    Then in "<folder>" there is one message
      | mid | labels        | revision |
      | $1  | system:pinned | 3        |
    And "system" label "pinned" has "0" messages at revision "1"

    Examples:
      | folder |
      | trash  |
      | spam   |

  Scenario: Set label on messages from inbox and spam
    Given new initialized user with "$1" in "inbox"
    When we store "$2" into "trash"
    And we set "+system:pinned" on "$1, $2"
    Then in "inbox" there is one message
      | mid | labels        | revision |
      | $1  | system:pinned | 4        |
    And in "trash" there is one message
      | mid | labels        | revision |
      | $2  | system:pinned | 4        |
    And "system" label "pinned" has one message at revision "4"

  Scenario: Update write to changelog with request_info default to null
    Given new initialized user with "$1" in "inbox"
    When we set "+seen" on "$1"
    Then in changelog there are
      | revision | type   | mids | x_request_id | session_key |
      | 3        | update | $1   |              |             |

  Scenario: Update write to changelog with request_info default to null
    Given new initialized user with "$1" in "inbox"
    When we set request_info "(x-request-id,session-key)"
    And we set "+seen" on "$1"
    Then in changelog there are
      | revision | type   | mids | x_request_id | session_key |
      | 3        | update | $1   | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Update write useful_new_count to changelog
    Given new initialized user with "$1" in "inbox"
    When we store "$2" into "trash"
    And we set "+seen" on "$2"
    Then "update" is last changelog entry with "1" as useful_new_count

  @changelog @changed
  Scenario: Update writes valid changed to changelog
    Given new initialized user with "$1" in "inbox"
    When we set "+seen" on "$1"
    Then last changelog.changed matches "changed/update_messages.json" schema
    And last changelog.arguments matches "arguments/update_messages.json" schema

  Scenario: Should change message_seen counter in case of labeling and unlabeling seen message
    Given new initialized user with "$1" in "inbox"
    When we set "<flag>" on "$1"
    And we create "user" label "apple" with "0" message_seen
    And we set "+user:apple" on "$1"
    Then "user" label "apple" has "<count>" seen messages
    When we set "-user:apple" on "$1"
    Then "user" label "apple" has "<count_after>" seen messages
    And "user" label "apple" has "<count_after>" messages
    Examples:
      | flag  | count | count_after |
      | +seen | 1     | 0           |
      | -seen | 0     | 0           |

  Scenario: Should decrease message_seen counter after removing message
    Given new initialized user with "$1" in "inbox"
    When we create "user" label "apple" with "0" message_seen
    And we set "+user:apple" on "$1"
    When we set "+seen" on "$1"
    Then "user" label "apple" has "1" seen messages
    When we delete "$1"
    Then "user" label "apple" has "0" seen messages

  Scenario: Should change message_seen counter in case of setting seen
    Given new initialized user with "$1" in "inbox"
    When we create "user" label "apple" with "0" message_seen
    And we set "+user:apple" on "$1"
    Then "user" label "apple" has "0" seen messages
    When we set "+seen" on "$1"
    Then "user" label "apple" has "1" seen messages
    When we set "-seen" on "$1"
    Then "user" label "apple" has "0" seen messages
