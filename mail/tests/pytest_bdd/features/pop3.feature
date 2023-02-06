Feature: pop3 actions

  Scenario: New user has no pop3
    Given new initialized user
    Then he has no pop3 folders
    And pop3 box is empty
    When we store "$1" into "inbox"
    Then pop3 box is empty

  Scenario: Enabled, disable and initialize pop3 for inbox
    Given new initialized user
    When we enable pop3 for "inbox"
    Then pop3 is enabled for "inbox" at revision "2"
    And pop3 is not initialized for "inbox"
    When we disable pop3 for "inbox"
    Then pop3 is disabled and not initialized for "inbox" at revision "3"
    When we initialize pop3 for "inbox"
    Then pop3 is initialized and disabled for "inbox" at revision "4"

  Scenario Outline: Toggle and initialize pop3 idempotency
    Given new initialized user
    When we enable pop3 for "inbox"
    Then pop3 is enabled for "inbox"
    When we <action> pop3 for "inbox"
    Then pop3 is <state> for "inbox"
    When we <action> pop3 for "inbox"
    Then pop3 is <state> for "inbox"
    Examples:
      | action     | state                        |
      | enable     | enabled and not initialized  |
      | disable    | disabled and not initialized |
      | initialize | initialized and enabled      |

  Scenario: Initialize pop3 after store
    Given new initialized user
    When we enable pop3 for "inbox"
    And we store "$1" into "inbox"
    Then pop3 box is empty
    And pop3 is enabled for "inbox"
    And pop3 is not initialized for "inbox"
    When we initialize pop3 for "inbox"
    Then pop3 is enabled and initialized for "inbox"
    Then pop3 box is
      | mid | folder |
      | $1  | inbox  |

  Scenario: POP3 actions write to changelog
    Given new initialized user
    When we enable pop3 for "inbox"
    And we initialize pop3 for "inbox"
    And we disable pop3 for "inbox"
    Then in changelog there are
      | revision | type                       |
      | 2        | pop3-folders-enable        |
      | 3        | pop3-folder-initialization |
      | 4        | pop3-folders-disable       |

  @MAILPG-932 @useful_new_count
  Scenario: POP3 actions write useful_new_count to changelog
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we enable pop3 for "inbox"
    And we initialize pop3 for "inbox"
    And we disable pop3 for "inbox"
    Then in changelog there are
      | revision | type                       | useful_new_count |
      | 5        | pop3-folders-enable        | 2                |
      | 6        | pop3-folder-initialization | 2                |
      | 7        | pop3-folders-disable       | 2                |

  @other-user
  Scenario: POP3 actions do not touch other users
    Given new initialized user
    And replication stream
    When we enable pop3 for "inbox"
    And we initialize pop3 for "inbox"
    And we disable pop3 for "inbox"
    Then there are only our user changes in replication stream

  Scenario: Move message into folder with pop3 enabled
    Given new user with popped "drafts"
    When we store into "inbox"
      | mid | attributes |
      | $1  |            |
      | $2  | spam       |
    Then pop3 box is empty
    When we move "$1" to "drafts"
    Then pop3 box is
      | mid | folder |
      | $1  | drafts |
    When we move "$2" to "drafts"
    And we move "$1" to "trash"
    Then pop3 box is
      | mid | folder |
      | $2  | drafts |

  Scenario: Move message from pop3 to pop3 folder
    Given new user with popped "inbox"
    When we enable and initialize pop3 for "drafts"
    And we store into "inbox"
      | mid | attributes |
      | $1  |            |
      | $2  | spam       |
    Then pop3 box is
      | mid | folder |
      | $1  | inbox  |
      | $2  | inbox  |
    When we move "$[1:2]" to "drafts"
    Then pop3 box is
      | mid | folder |
      | $1  | drafts |
      | $2  | drafts |

  Scenario: Delete message from two popped folder
    Given new user with popped "inbox"
    When we enable and initialize pop3 for "drafts"
    And we store "$1, $2" into "inbox"
    And we store "$3" into "drafts"
    Then pop3 box is "$[1:3]"
    When we delete "$[2:3]"
    Then pop3 box is "$1"

  Scenario: Copy message into pop3-folder
    Given new user with popped "drafts"
    When we store into "inbox"
      | mid | attributes |
      | $1  | spam       |
      | $2  |            |
      | $3  | spam       |
    And we copy "$[1:3]" to "drafts" new message "$[1:3]c" appears
    Then pop3 box is
      | mid | folder |
      | $1c | drafts |
      | $2c | drafts |
      | $3c | drafts |

  Scenario: Initialize not empty folder and delete messages from it
    Given new initialized user
    When we store into "inbox"
      | mid | attributes |
      | $1  | spam       |
      | $2  |            |
      | $3  | spam       |
    Then pop3 box is empty
    When we enable and initialize pop3 for "inbox"
    Then pop3 box is
      | mid | folder |
      | $1  | inbox  |
      | $2  | inbox  |
      | $3  | inbox  |
    When we delete "$[1:3]"
    Then pop3 box is empty

  Scenario: Actions on message from popped folder increase their revisions
    Given new user with popped "inbox"
    Then global revision is "3"
    And pop3 "inbox" at revision "3"
    When we store "$1" into "inbox"
      | flags |
      | seen  |
    Then pop3 "inbox" at revision "4"
    When we store "$2" into "drafts"
    Then global revision is "5"
    And pop3 "inbox" at revision "4"
    When we move "$2" to "inbox"
    Then pop3 "inbox" at revision "6"
    When we set "-seen" on "$1"
    Then pop3 "inbox" at revision "7"
    When we delete "$2"
    Then pop3 "inbox" at revision "8"
    When we enable pop3 for "trash"
    Then pop3 "trash" at revision "9"
    When we move "$1" to "trash"
    Then pop3 "trash" at revision "10"
    And pop3 "inbox" at revision "10"

  Scenario: Use pop-delete
    Given new user with popped "inbox"
    When we enable and initialize pop3 for "spam"
    Then global revision is "5"
    When we store "$[1:3]" into "inbox"
    And we store "$[4:6]" into "spam"
    Then global revision is "11"
    And pop3 box is "$[1:6]"
    When we pop3-delete "$1"
    Then global revision is "12"
    And pop3 box is "$[2:6]"
    And pop3 "inbox" at revision "12"
    When we pop3-delete "$[2:5]"
    Then global revision is "13"
    And pop3 box is "$6"
    And pop3 "inbox" at revision "13"
    And pop3 "spam" at revision "13"
    And in changelog there are
      | revision | type        |
      | 12       | pop3-delete |
      | 13       | pop3-delete |

  @MAILPG-932 @useful_new_count
  Scenario: Pop-delete write useful_new_count to changelog
    Given new user with popped "inbox"
    When we store "$1, $2" into "inbox"
    And we store "$3" into "trash"
    And we pop3-delete "$1, $2"
    Then "pop3-delete" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Pop-delete do not touch other user
    Given new user with popped "inbox"
    And replication stream
    When we store "$1" into "inbox"
    And we pop3-delete "$1"
    Then there are only our user changes in replication stream
