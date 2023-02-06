Feature: Move messages to tab

  Background: New initialized user with messages in all tabs
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid | size | flags |
      | $1  | 42  | 10   |       |
      | $2  | 42  | 20   | seen  |
      | $3  | 13  | 30   |       |
    And we store into tab "news"
      | mid | tid | size | flags         |
      | $4  | 42  | 40   |               |
      | $5  | 42  | 50   | seen, deleted |
    And we store into tab "social"
      | mid | tid | size | flags |
      | $6  | 13  | 60   | seen  |
    And we set "-recent" on "$2, $5, $6"
    And we store into "inbox"
      | mid | tid | size | flags |
      | $7  | 13  | 70   | seen  |
      | $8  | 42  | 80   |       |

  Scenario: Move to tab many sanity check
    Then global revision is "10"
    And tab "relevant" has "3" messages, "2" unseen, "60" size at revision "8"
    And tab "news" has "2" messages, "1" unseen, "90" size at revision "8"
    And tab "social" has "1" message, "0" unseen, "60" size at revision "8"
    And "inbox" has "8" messages, "4" unseen, "360" size at revision "10"
    And in tab "relevant" there are "3" messages
      | mid | tid | revision | flags  |
      | $1  | 42  | 2        | recent |
      | $2  | 42  | 8        | seen   |
      | $3  | 13  | 4        | recent |
    And in tab "news" there are "2" messages
      | mid | tid | revision | flags         |
      | $4  | 42  | 5        | recent        |
      | $5  | 42  | 8        | seen, deleted |
    And in tab "social" there is one message
      | mid | tid | revision | flags |
      | $6  | 13  | 8        | seen  |
    And in "inbox" there are "8" messages
      | mid | tid | revision | flags         |
      | $1  | 42  | 2        | recent        |
      | $2  | 42  | 8        | seen          |
      | $3  | 13  | 4        | recent        |
      | $4  | 42  | 5        | recent        |
      | $5  | 42  | 8        | seen, deleted |
      | $6  | 13  | 8        | seen          |
      | $7  | 13  | 9        | seen, recent  |
      | $8  | 42  | 10       | recent        |
    And in tab "relevant" there are "2" threads
      | tid | mid | thread_revision | count | unseen |
      | 42  | $2  | 10              | 5     | 3      |
      | 13  | $3  | 9               | 3     | 1      |
    And in tab "news" there is one thread
      | tid | mid | thread_revision | count | unseen |
      | 42  | $5  | 10              | 5     | 3      |
    And in tab "social" there is one thread
      | tid | mid | thread_revision | count | unseen |
      | 13  | $6  | 9               | 3     | 1      |
    And in "inbox" there are "2" threads
      | tid | mid | thread_revision | count | unseen |
      | 13  | $7  | 9               | 3     | 1      |
      | 42  | $8  | 10              | 5     | 3      |

  Scenario: Move all messages from their tabs to a single tab
    When we move "$[1:8]" to tab "news"
    Then global revision is "11"
    And tab "relevant" is empty at revision "11"
    And tab "social" is empty at revision "11"
    And tab "news" has "8" messages, "4" unseen, "360" size at revision "11"
    And in tab "news" there are "8" messages
      | mid | tid | revision | flags         |
      | $1  | 42  | 11       | recent        |
      | $2  | 42  | 11       | seen          |
      | $3  | 13  | 11       | recent        |
      | $4  | 42  | 5        | recent        |
      | $5  | 42  | 8        | seen, deleted |
      | $6  | 13  | 11       | seen          |
      | $7  | 13  | 11       | seen, recent  |
      | $8  | 42  | 11       | recent        |
    And in tab "news" there are "2" threads
      | tid | mid | count | unseen | thread_revision |
      | 42  | $8  | 5     | 3      | 11              |
      | 13  | $7  | 3     | 1      | 11              |

  Scenario: Move messages from a tab to null and then to another tab
    When we move "$1, $2, $5, $7" to null tab
    Then global revision is "11"
    And tab "relevant" has "1" message, "1" unseen, "30" size at revision "11"
    And tab "news" has "1" message, "1" unseen, "40" size at revision "11"
    And tab "social" has "1" message, "0" unseen, "60" size at revision "8"
    And in tab "relevant" there is one message
      | mid | tid | revision | flags  |
      | $3  | 13  | 4        | recent |
    And in tab "news" there is one message
      | mid | tid | revision | flags  |
      | $4  | 42  | 5        | recent |
    And in tab "social" there is one message
      | mid | tid | revision | flags |
      | $6  | 13  | 8        | seen  |
    And in tab "relevant" there is one thread
      | tid | mid | thread_revision | count | unseen |
      | 13  | $3  | 9               | 3     | 1      |
    And in tab "news" there is one thread
      | tid | mid | thread_revision | count | unseen |
      | 42  | $4  | 11              | 5     | 3      |
    And in tab "social" there is one thread
      | tid | mid | thread_revision | count | unseen |
      | 13  | $6  | 9               | 3     | 1      |
    When we move "$3, $4, $6, $8" to null tab
    Then global revision is "12"
    And tab "relevant" is empty at revision "12"
    And tab "news" is empty at revision "12"
    And tab "social" is empty at revision "12"
    When we move "$[1:8]" to tab "relevant"
    Then global revision is "13"
    And tab "news" is empty at revision "12"
    And tab "social" is empty at revision "12"
    And tab "relevant" has "8" messages, "4" unseen, "360" size at revision "13"
    And in tab "relevant" there are "8" messages
      | mid | tid | revision | flags         |
      | $1  | 42  | 13       | recent        |
      | $2  | 42  | 13       | seen          |
      | $3  | 13  | 13       | recent        |
      | $4  | 42  | 13       | recent        |
      | $5  | 42  | 13       | seen, deleted |
      | $6  | 13  | 13       | seen          |
      | $7  | 13  | 13       | seen, recent  |
      | $8  | 42  | 13       | recent        |
    And in tab "relevant" there are "2" threads
      | tid | mid | count | unseen | thread_revision |
      | 42  | $8  | 5     | 3      | 13              |
      | 13  | $7  | 3     | 1      | 13              |
