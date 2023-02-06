Feature: Concurrent move and store messages

  Background: All on new user with one message in inbox
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
    Then "inbox" has one message at revision "2"

  Scenario Outline: Move message from inbox and try to store into inbox
    When we try move "$1" to "trash" as "$move"
    And we try store "$2" into "inbox" as "$store"
    When we <move_do> "$move"
    And we <store_do> "$store"
    Then "inbox" has "<inbox_count>" messages at revision "<inbox_revision>"
    And "trash" has "<trash_count>" messages at revision "<trash_revision>"
    And global revision is "<global_revision>"

    Examples:
      | move_do  | store_do | inbox_count | trash_count | inbox_revision | trash_revision | global_revision |
      | commit   | commit   | 1           | 1           | 4              | 3              | 4               |
      | commit   | rollback | 0           | 1           | 3              | 3              | 3               |
      | rollback | commit   | 2           | 0           | 3              | 1              | 3               |
      | rollback | rollback | 1           | 0           | 2              | 1              | 2               |

  Scenario Outline: Move thread from inbox and store into drafts in this thread
    When we try move "$1" to "<dest_folder>" as "$move"
    And we try store "$2" into "drafts" as "$store"
      | tid |
      | 1   |
    When we commit "$move"
    And we commit "$store"
    Then "inbox" has not messages
    And in "drafts" there is one thread
      | tid | mid | count          |
      | 1   | $2  | <thread_count> |
    And "<dest_folder>" has one message

    Examples:
      | dest_folder | thread_count |
      | sent        | 2            |
      | trash       | 1            |
      | spam        | 1            |
