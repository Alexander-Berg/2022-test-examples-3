Feature: Copy messages

  Background: All on new initialized user with one message in inbox
    Given new initialized user
    When we store "$1" into "inbox"
      | tid | size | attaches                  |
      | 42  | 10   | 1.2:image/jpg:apple.jpg:0 |

  Scenario: Copy sanity check
    Then global revision is "2"
    And in "inbox" there is one message
      | mid | tid | size | revision | attributes | recent | imap_id |
      | $1  | 42  | 10   | 2        |            | true   | 1       |
    And in "inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 2               | 1     |
    And user has one message with attaches, not seen

  Scenario: Copy deleted
    When we delete "$1"
    And we try copy "$1" to "inbox" as "$copy" new message "$2" can appears
    And we commit "$copy"
    Then "$copy" result has unchanged revision
    And global revision is "3"

  Scenario: Copy from inbox to inbox
    When we set "-recent" on "$1"
    And we copy "$1" to "inbox" new message "$2" appears
    Then global revision is "4"
    And in "inbox" there are "2" messages
      | mid | tid | revision | attributes | recent | imap_id |
      | $1  | 42  | 3        |            | false  | 1       |
      | $2  | 42  | 4        | copy       | true   | 2       |
    And in "inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 4               | 2     |
    And user has "2" messages with attaches, not seen

  Scenario: Copy from inbox to drafts
    When we set "-recent" on "$1"
    And we copy "$1" to "drafts" new message "$2" appears
    Then global revision is "4"
    And in "inbox" there is one message
      | mid | tid | recent | revision | attributes | imap_id |
      | $1  | 42  | false  | 3        |            | 1       |
    And in "drafts" there is one message
      | mid | tid | recent | revision | attributes | imap_id |
      | $2  | 42  | true   | 4        | copy       | 1       |
    And in folders "drafts, inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 4               | 2     |

  Scenario: Copy from inbox to trash and back
    When we set "-recent" on "$1"
    And we copy "$1" to "trash" new message "$2" appears
    Then global revision is "4"
    And in "inbox" there is one message
      | mid | tid | recent | revision | attributes | imap_id |
      | $1  | 42  | false  | 3        |            | 1       |
    And in "trash" there is one message
      | mid | tid | recent | revision | attributes | imap_id |
      | $2  |     | true   | 4        | copy       | 1       |
    And in "inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 3               | 1     |
    And in "trash" there are no threads
    And user has one message with attaches, not seen
    When we copy "$2" to "inbox" new message "$3" appears
    Then global revision is "5"
    And in "inbox" there are "2" messages
      | mid | tid | recent | revision | attributes | imap_id |
      | $1  | 42  | false  | 3        |            | 1       |
      | $3  | 42  | true   | 5        | copy       | 2       |
    And in "trash" there is one message
      | mid | tid | recent | revision | attributes | imap_id |
      | $2  |     | true   | 4        | copy       | 1       |
    And in "inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 5               | 2     |
    And user has "2" messages with attaches, not seen

  Scenario: Copy write to changelog
    When we copy "$1" to "drafts" new message "$2" appears
    And we copy "$1, $2" to "drafts" new messages "$1c, $2c" appears
    Then in changelog there are
      | revision | type | mids     |
      | 3        | copy | $2       |
      | 4        | copy | $1c, $2c |

  Scenario: Copy write to changelog with request_info default to NULL
    When we copy "$1" to "drafts" new message "$2" appears
    Then in changelog there are
      | revision | type | x_request_id | session_key |
      | 3        | copy |              |             |

  Scenario: Copy write to changelog with request_info
    When we set request_info "(x-request-id,session-key)"
    And we copy "$1" to "drafts" new message "$2" appears
    Then in changelog there are
      | revision | type | x_request_id | session_key |
      | 3        | copy | x-request-id | session-key |

  @changelog @changed
  Scenario: Copy writes valid changed to changelog
    When we copy "$1" to "drafts" new message "$2" appears
    Then last changelog.changed matches "changed/store_message.json" schema

  Scenario: Copy message with label to trash
    When we create "user" label "arch"
    And we set "+user:arch" on "$1"
    Then global revision is "4"
    Then "user" label "arch" has one message at revision "4"
    When we copy "$1" to "trash" new message "$1c" appears
    Then in "trash" there is one message
      | mid | label     |
      | $1c | user:arch |
    And "user" label "arch" has one message at revision "4"

  Scenario Outline: Set doom_date while copy
    When we copy "$1" to "<folder>" new message "$2" appears
    Then message "$2" has recent doom_date
    When we copy "$2" to "inbox" new message "$3" appears
    Then message "$3" has null doom_date
    Examples:
      | folder |
      | spam   |
      | trash  |

  @other-user
  Scenario Outline: Copy does not touch other users
    Given replication stream
    When we copy "$1" to "<folder>" new message "$2" appears
    Then there are only our user changes in replication stream
    Examples:
      | folder |
      | spam   |
      | drafts |

  Scenario: Copy message from inbox to tab
    When we copy "$1" to tab "news" as "$2"
    Then global revision is "3"
    And in "inbox" there are "2" messages
      | mid | tid | revision | attributes | imap_id |
      | $1  | 42  | 2        |            | 1       |
      | $2  | 42  | 3        | copy       | 2       |
    And in tab "news" there is one message
      | mid | tid | revision | attributes |
      | $2  | 42  | 3        | copy       |
    And in "inbox" there is one thread
      | tid | thread_revision | count |
      | 42  | 3               | 2     |
    And in tab "news" there is one thread
      | tid | thread_revision | count |
      | 42  | 3               | 2     |
