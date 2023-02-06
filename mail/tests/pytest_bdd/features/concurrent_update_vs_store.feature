Feature: Concurrent update and store messages

  Background: All on new user with one message in inbox
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
    Then "inbox" has one message, one unseen, one recent at revision "2"

  Scenario Outline: Store and update in inbox, update first
    When we try set "+seen" on "$1" as "$update"
    And we try store "$2" into "inbox" as "$store"
    When we <update_do> "$update"
    And we <store_do> "$store"
    Then "inbox" has "<count>" messages, "<unseen>" unseen at revision "<rev>"

    Examples:
      | update_do | store_do | count | unseen | rev |
      | commit    | commit   | 2     | 1      | 4   |
      | commit    | rollback | 1     | 0      | 3   |
      | rollback  | commit   | 2     | 2      | 3   |
      | rollback  | rollback | 1     | 1      | 2   |

  Scenario Outline: Store and update in inbox, store first
    When we try store "$2" into "inbox" as "$store"
    And we try set "+seen" on "$1" as "$update"
    When we <store_do> "$store"
    And we <update_do> "$update"
    Then "inbox" has "<count>" messages, "<unseen>" unseen at revision "<rev>"

    Examples:
      | update_do | store_do | count | unseen | rev |
      | commit    | commit   | 2     | 1      | 4   |
      | commit    | rollback | 1     | 0      | 3   |
      | rollback  | commit   | 2     | 2      | 3   |
      | rollback  | rollback | 1     | 1      | 2   |

  Scenario: Update thread from inbox and store in same thread to drafts
    When we try set "+seen" on "$1" as "$update"
    And we try store "$2" into "drafts" as "$store"
      | tid |
      | 1   |
    When we commit "$update"
    And we commit "$store"
    Then in "inbox" there is one thread
      | tid | mid | count | unseen | flags        |
      | 1   | $1  | 2     | 1      | seen, recent |
    And in "drafts" there is one thread
      | tid | mid | count | unseen | flags  |
      | 1   | $2  | 2     | 1      | recent |

  Scenario: Store into drafts and update same thread
    When we try store "$2" into "drafts" as "$store"
      | tid |
      | 1   |
    And we try set "+seen" on "$1" as "$update"
    When we commit "$store"
    And we commit "$update"
    Then in "inbox" there is one thread
      | tid | mid | count | unseen | flags        |
      | 1   | $1  | 2     | 1      | seen, recent |
    And in "drafts" there is one thread
      | tid | mid | count | unseen | flags  |
      | 1   | $2  | 2     | 1      | recent |
