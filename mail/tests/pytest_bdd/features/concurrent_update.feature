Feature: Concurrent messages update

  Background: New user
    Given new initialized user

  Scenario Outline: Update 2 messages in one folder
    When we store "$1, $2" into "inbox"
    Then "inbox" has "2" messages, "2" unseen, "2" recent at revision "3"
    And in "inbox" there are "2" messages
      | mid | revision |
      | $1  | 2        |
      | $2  | 3        |
    When we try set "<flags>" on "$1" as "$first"
    And we try set "<flags>" on "$2" as "$second"
    When we <first_do> "$first"
    And we <second_do> "$second"
    Then "inbox" has "2" messages, "<unseen>" unseen, "<recent>" recent at revision "<rev>"
    And in "inbox" there are "2" messages
      | mid | revision |
      | $1  | <rev1>   |
      | $2  | <rev2>   |

    Examples:
      | flags   | first_do | second_do | unseen | recent | rev | rev1 | rev2 |
      | +seen   | COMMIT   | COMMIT    | 0      | 2      | 5   | 4    | 5    |
      | +seen   | COMMIT   | ROLLBACK  | 1      | 2      | 4   | 4    | 3    |
      | +seen   | ROLLBACK | COMMIT    | 1      | 2      | 4   | 2    | 4    |
      | +seen   | ROLLBACK | ROLLBACK  | 2      | 2      | 3   | 2    | 3    |
      | -recent | COMMIT   | COMMIT    | 2      | 0      | 5   | 4    | 5    |
      | -recent | COMMIT   | ROLLBACK  | 2      | 1      | 4   | 4    | 3    |
      | -recent | ROLLBACK | COMMIT    | 2      | 1      | 4   | 2    | 4    |
      | -recent | ROLLBACK | ROLLBACK  | 2      | 2      | 3   | 2    | 3    |


  Scenario Outline: set deleted or recent for messages in one thread in different folders
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
    And we store into "drafts"
      | mid | tid |
      | $2  | 1   |
    When we try set "<flags_modify>" on "$1" as "$first"
    And we try set "<flags_modify>" on "$2" as "$second"
    When we commit "$first"
    And we commit "$second"
    Then in "inbox" there is one message
      | mid | flags            |
      | $1  | <flags_expected> |
    And in "drafts" there is one message
      | mid | flags            |
      | $2  | <flags_expected> |

    Examples:
      | flags_modify      | flags_expected |
      | +deleted          | recent,deleted |
      | -recent           |                |
      | +deleted, -recent | deleted        |


  Scenario Outline: set seen on message from one thread in different folders
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
    And we store into "drafts"
      | mid | tid |
      | $2  | 1   |
    When we try set "+seen" on "$1" as "$first"
    And we try set "+seen" on "$2" as "$second"
    When we <first_do> "$first"
    And we <second_do> "$second"
    Then in "inbox" there is one thread
      | tid | mid | count | unseen   | flags         |
      | 1   | $1  | 2     | <unseen> | <first_flags> |
    And in "drafts" there is one thread
      | tid | mid | count | unseen   | flags          |
      | 1   | $2  | 2     | <unseen> | <second_flags> |

    Examples:
      | first_do | second_do | first_flags  | second_flags | unseen |
      | commit   | commit    | seen, recent | seen, recent | 0      |
      | commit   | rollback  | seen, recent | recent       | 1      |
      | rollback | commit    | recent       | seen, recent | 1      |
      | rollback | rollback  | recent       | recent       | 2      |


  Scenario: Concurrent same message update
    When we store "$1" into "inbox"
    When we try set "+seen" on "$1" as "$first"
    And we try set "+seen" on "$1" as "$second"
    When we commit "$first"
    Then "$first" result has one row
      | mid | revision |
      | $1  | 3        |
    When we commit "$second"
    Then "$second" result has unchanged revision
