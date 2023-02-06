Feature: Operations with hidden_trash

  Scenario Outline: Set doom_date while move to hidden_trash
    Given new initialized user with "$1" in "trash"
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    Then message "$1" has recent doom_date
    When we move "$1" to "inbox"
    Then message "$1" has null doom_date

  Scenario: Move to hidden_trash and back
    Given new initialized user
    When we store "$1" into tab "relevant"
      | label        | tid | size | attaches                  |
      | system:draft | 42  | 10   | 1.2:image/jpg:apple.jpg:0 |
    And we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "trash"
    And we move "$1" to "hidden_trash"
    Then in "inbox" there are no threads
    And in "hidden_trash" there are no threads
    And in "hidden_trash" there is one message
      | mid | tid | label        | revision |
      | $1  |     | system:draft | 5        |
    And "system" label "draft" has "0" messages at revision "4"
    And user has no message with attaches
    When we move "$1" to "inbox"
    Then global revision is "6"
    And "inbox" has one message one recent at revision "6"
    And "hidden_trash" has no messages at revision "6"
    And in "inbox" there is one message
      | mid | tid | label        | revision |
      | $1  | 42  | system:draft | 6        |
    And in "inbox" there is one thread
      | mid | tid | label        | revision | count |
      | $1  | 42  | system:draft | 6        | 1     |
    And "system" label "draft" has one message at revision "6"
    And user has one message with attaches, not seen

  Scenario Outline: Delete trash messages for user with enabled hidden_trash 
    Given new initialized user with "$1" in "trash"
    And he has enabled hidden_trash
    When we create "hidden_trash" folder "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is not deleted
    And in "hidden_trash" there is one message

  Scenario Outline: Delete hidden_trash messages for user with enabled hidden_trash 
    Given new initialized user with "$1" in "trash"
    And he has enabled hidden_trash
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is deleted
    And in "hidden_trash" there are "0" messages

  Scenario Outline: Delete trash messages for user with disabled hidden_trash 
    Given new initialized user with "$1" in "trash"
    When we create "hidden_trash" folder "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is deleted
    And in "hidden_trash" there are "0" messages

  Scenario Outline: Delete hidden_trash messages for user with disabled hidden_trash 
    Given new initialized user with "$1" in "trash"
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is deleted
    And in "hidden_trash" there are "0" messages

  Scenario Outline: Delete messages for user with enabled hidden_trash 
    Given new initialized user with "$1, $2" in "trash"
    And he has enabled hidden_trash
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    And we delete "$1, $2"
    Then in "trash" there are "0" messages
    And "$1" is deleted
    And "$2" is not deleted
    And in "hidden_trash" there is one message
