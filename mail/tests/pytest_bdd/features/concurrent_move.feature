Feature: Move messages concurrently

  Background: New user
    Given new initialized user

  Scenario Outline: Move 2 messages in different sessions
    When we store "$1, $2" into "inbox"
    Then "inbox" has "2" messages, "2" unseen, "2" recent at revision "3"
    And in "inbox" there are "2" messages
      | mid | revision |
      | $1  | 2        |
      | $2  | 3        |
    When we try move "$1" to "trash" as "$first"
    And we try move "$2" to "trash" as "$second"
    And we <first_do> "$first"
    And we <second_do> "$second"
    Then "inbox" has "<inbox_count>" messages at revision "<inbox_rev>"
    And "trash" has "<trash_count>" messages at revision "<trash_rev>"
    And in "inbox" there are "<inbox_count>" messages
    And in "trash" there are "<trash_count>" messages

    Examples:
      | first_do | second_do | inbox_count | trash_count | inbox_rev | trash_rev |
      | commit   | commit    | 0           | 2           | 5         | 5         |
      | commit   | rollback  | 1           | 1           | 4         | 4         |
      | rollback | commit    | 1           | 1           | 4         | 4         |
      | rollback | rollback  | 2           | 0           | 3         | 1         |

  Scenario: Move same concurrently
    When we store "$1" into "inbox"
    When we try move "$1" to "trash" as "$first"
    And we try move "$1" to "trash" as "$second"
    When we commit "$first"
    Then "$first" result has one row
      | mid | revision |
      | $1  | 3        |
    When we commit "$second"
    Then "$second" result has unchanged revision
