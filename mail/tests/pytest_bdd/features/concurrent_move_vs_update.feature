Feature: Concurrent move and update messages

  Background: All on new user with one message in inbox
    Given new initialized user
    When we store "$1" into "inbox"
    Then "inbox" has one message at revision "2"

  Scenario Outline: Move message from inbox and try set flags on messages from inbox
    When we store "$2" into "inbox"
    Then "inbox" has "2" messages at revision "3"
    When we try move "$1" to "trash" as "$move"
    And we try set "+seen" on "$2" as "$update"
    When we <move_do> "$move"
    And we <update_do> "$update"
    Then "inbox" has "<inbox_count>" messages at revision "<inbox_revision>"
    And "trash" has "<trash_count>" messages at revision "<trash_revision>"
    And global revision is "<global_revision>"

    Examples:
      | move_do  | update_do | inbox_count | trash_count | inbox_revision | trash_revision | global_revision |
      | commit   | commit    | 1           | 1           | 5              | 4              | 5               |
      | commit   | rollback  | 1           | 1           | 4              | 4              | 4               |
      | rollback | commit    | 2           | 0           | 4              | 1              | 4               |
      | rollback | rollback  | 2           | 0           | 3              | 1              | 3               |

  Scenario: Move message from inbox and set flags on it
    When we try move "$1" to "trash" as "$move"
    And we try set "+seen" on "$1" as "$update"
    When we commit "$move"
    And we commit "$update"
    Then "inbox" has not messages at revision "3"
    And in "trash" there is one message
      | mid | flags        | revision |
      | $1  | recent, seen | 4        |
