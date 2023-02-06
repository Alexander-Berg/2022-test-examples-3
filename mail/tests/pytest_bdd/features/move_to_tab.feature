Feature: Move messages to tab

  Scenario: Move messages sanity check
    Given new initialized user with "$1" in tab "relevant"
    Then tab "relevant" has one message, "1" unseen at revision "2"
    And global revision is "2"
    And "inbox" has one message at revision "2"

  Scenario: Move from tab to tab
    Given new initialized user with "$1" in tab "relevant"
    When we move "$1" to tab "news"
    Then global revision is "3"
    And "inbox" has one message at revision "3"
    And tab "relevant" is empty at revision "3"
    And tab "news" has one message at revision "3"
    And in tab "news" there is one message
      | mid | revision |
      | $1  | 3        |

  Scenario: Move seen message does not make it unseen
    Given new initialized user with "$1" in tab "relevant"
    When we set "+seen" on "$1"
    And we move "$1" to tab "social"
    Then tab "relevant" is empty at revision "4"
    And tab "social" has one message, "0" unseen at revision "4"
    And in tab "social" there is one message
      | mid | revision | flags        |
      | $1  | 4        | seen, recent |

  Scenario: Move from tab to null
    Given new initialized user with "$1" in tab "relevant"
    When we move "$1" to null tab
    Then global revision is "3"
    And tab "relevant" is empty at revision "3"
    And "inbox" has one messages one recent at revision "3"
    And in "inbox" there is one message
      | mid | revision |
      | $1  | 3        |

  Scenario: Move from null to tab
    Given new initialized user with "$1" in "inbox"
    When we move "$1" to tab "relevant"
    Then global revision is "3"
    And tab "relevant" has one message at revision "3"
    And in tab "relevant" there is one message
      | mid | revision |
      | $1  | 3        |

  Scenario: Move idempotency
    Given new initialized user with "$1" in tab "relevant"
    When we try move "$1" to tab "relevant" as "$move"
    And we commit "$move"
    Then "$move" result has unchanged revision
    And global revision is "2"

  @changelog @changed
  Scenario: Move writes valid changed to changelog
    Given new initialized user with "$1" in tab "relevant"
    When we set request_info "(x-request-id,session-key)"
    And we move "$1" to null tab
    Then in changelog there are
      | revision | type | x_request_id | session_key |
      | 3        | move | x-request-id | session-key |
    And last changelog.changed matches "changed/move_messages.json" schema
    And last changelog.arguments matches "arguments/move_messages.json" schema

  @other-user
  Scenario: Move do not touch other users
    Given new initialized user with "$1" in tab "relevant"
    And replication stream
    When we move "$1" to tab "news"
    Then there are only our user changes in replication stream

  Scenario: Move updates threads layout for tabs
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid | size |
      | $1  | 42  | 10   |
      | $2  | 42  | 15   |
      | $3  | 42  | 20   |
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $3  | 42  |
    And tab "news" is empty at revision "1"
    When we move "$2" to tab "news"
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $3  | 42  |
    And in tab "news" there is one thread
      | mid | tid |
      | $2  | 42  |
    When we move "$3" to tab "news"
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $1  | 42  |
    And in tab "news" there is one thread
      | mid | tid |
      | $3  | 42  |
    When we move "$1" to tab "news"
    Then tab "relevant" is empty at revision "7"
    And in tab "news" there is one thread
      | mid | tid |
      | $3  | 42  |

  @MAILPG-3868
  Scenario: Move newest message in thread from tab
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid |
      | $1  | 42  |
      | $2  | 42  |
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $2  | 42  |
    When we move "$2" to "trash"
    Then in tab "relevant" there is one thread
      | mid | tid |
      | $1  | 42  |

  @MAILPG-3973
  Scenario: Move messages both with and without folder change (first is newer)
    Given new initialized user
    When we store "$1" into "sent"
    And we store "$2" into tab "relevant"
    And we move "$[1:2]" to tab "news"
    Then in tab "news" there are "2" messages
    And check produce nothing

  @MAILPG-3973
  Scenario: Move messages both with and without folder change (second is newer)
    Given new initialized user
    When we store "$1" into tab "relevant"
    And we store "$2" into "sent"
    And we move "$[1:2]" to tab "news"
    Then in tab "news" there are "2" messages
    And check produce nothing
