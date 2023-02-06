Feature: Different utility functions

  Scenario: Fill changelog fro msearch [MDB-749]
    Given new initialized user
    When we store "$[2:8]" into "inbox"
    Then global revision is "8"
    When we fill changelog for msearch
    Then global revision is "8"
    And in changelog there are reindex changes
      | revision | mids       |
      | 4        | $2, $3, $4 |
      | 7        | $5, $6, $7 |
      | 8        | $8         |

  @MAILPG-1011
  Scenario: Delete folders subtree
       Create folder tree like
               a
              /  \
             b    f
           /   \
          c     d
                 \
                  e
    Given new initialized user
    When he create "user" folder "a"
    And he create "user" folder "b" under "a"
    And he create "user" folder "c" under "a|b"
    And he create "user" folder "d" under "a|b"
    And he create "user" folder "e" under "a|b|d"
    And he create "user" folder "f" under "a"
    When we delete folders subtree from folder named "a"
    Then folder named "a" does not exist

  @MAILPG-1011
  Scenario: Delete folders subtree, move messages to inbox with tab relevant
    Given new initialized user
    When he create "user" folder "a"
    And he create "user" folder "b" under "a"
    And we store "$1" into folder named "a"
    And we store "$2" into folder named "a|b"
    When we delete folders subtree from folder named "a"
    Then folder named "a" does not exist
    And in "inbox" there are "2" messages
      | mid |
      | $1  |
      | $2  |
    And in tab "relevant" there are "2" messages
      | mid |
      | $1  |
      | $2  |
