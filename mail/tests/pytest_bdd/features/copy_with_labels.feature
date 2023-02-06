Feature: Copy messages with labels

  Background: All on new initialized user with one message in inbox
    Given new initialized user
    When we store "$1" into "inbox"
      | tid | label        | size |
      | 42  | system:draft | 10   |

  Scenario: Sanity check after background
    Then global revision is "2"
    And in "inbox" there is one message
      | mid | tid | label        | size | flags  | attributes | imap_id |
      | $1  | 42  | system:draft | 10   | recent |            | 1       |
    And in "inbox" there is one thread
      | tid | label        | revision | count |
      | 42  | system:draft | 2        | 1     |
    And "system" label "draft" has one message at revision "2"

  Scenario: Copy from inbox to inbox
    When we copy "$1" to "inbox" new message "$2" appears
    Then global revision is "3"
    And in "inbox" there are "2" messages
      | mid | tid | label        | revision | attributes | recent | imap_id |
      | $1  | 42  | system:draft | 2        |            | true   | 1       |
      | $2  | 42  | system:draft | 3        | copy       | true   | 2       |
    And in "inbox" there is one thread
      | tid | label        | revision | count |
      | 42  | system:draft | 2        | 2     |
    And "system" label "draft" has "2" messages

  Scenario: Copy from inbox to drafts
    When we copy "$1" to "drafts" new message "$2" appears
    Then "inbox" has one message at revision "2"
    And in "inbox" there is one message
      | mid | tid | label        | size | revision | attributes | recent | imap_id |
      | $1  | 42  | system:draft | 10   | 2        |            | true   | 1       |
    And in "inbox" there is one thread
      | tid | label        | revision | count |
      | 42  | system:draft | 2        | 2     |
    And in "drafts" there is one message
      | mid | tid | label        | size | revision | attributes | imap_id |
      | $2  | 42  | system:draft | 10   | 3        | copy       | 1       |
    And in "drafts" there is one thread
      | tid | label        | revision | count |
      | 42  | system:draft | 3        | 2     |
    And "system" label "draft" has "2" messages at revision "3"

  Scenario Outline: Modify flags before copy
    When we set "<flags>" on "$1"
    Then "inbox" has one message, "<recent>" recent, "<unseen>" unseen, first_unseen at "<first_unseen>", at revision "3"
    When we copy "$1" to "drafts" new message "$2" appears
    Then "drafts" has one message, one recent, "<unseen>" unseen, first_unseen at "<first_unseen>", at revision "4"
    And in "drafts" there is one message
      | mid | tid | revision | flags         | attributes | imap_id |
      | $2  | 42  | 4        | <final_flags> | copy       | 1       |
    And in "drafts" there is one thread
      | tid | revision | flags         | message_unseen  |
      | 42  | 4        | <final_flags> | <thread_unseen> |

    Examples:
      | flags          | recent | unseen | thread_unseen | first_unseen | final_flags  |
      | +seen          | 1      | 0      | 0             | 0            | recent, seen |
      | -recent        | 0      | 1      | 2             | 1            | recent       |
      | +seen, -recent | 0      | 0      | 0             | 0            | recent, seen |

  Scenario: Copy from inbox to trash and back
    When we copy "$1" to "trash" new message "$2" appears
    Then global revision is "3"
    And in "trash" there is one message
      | mid | tid | revision | attributes | imap_id |
      | $2  |     | 3        | copy       | 1       |
    And "system" label "draft" has one message at revision "2"
    When we copy "$2" to "inbox" new message "$3" appears
    Then global revision is "4"
    And in "inbox" there are "2" messages
      | mid | tid | label        | revision | attributes | imap_id |
      | $1  | 42  | system:draft | 2        |            | 1       |
      | $3  | 42  | system:draft | 4        | copy       | 2       |
    And in "inbox" there is one thread
      | tid | label        | revision | count |
      | 42  | system:draft | 2        | 2     |
    And "system" label "draft" has "2" messages at revision "4"
