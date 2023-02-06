Feature: Operations with admin_search

  Scenario Outline: Delete trash messages for user with enabled admin_search 
    Given new initialized user with "$1" in "trash"
    And he has enabled admin_search
    When we create "hidden_trash" folder "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is not deleted
    And in "hidden_trash" there is one message

  Scenario Outline: Do not delete hidden_trash messages for user with enabled admin_search 
    Given new initialized user with "$1" in "trash"
    And he has enabled admin_search
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    And we delete "$1"
    Then in "trash" there are "0" messages
    And "$1" is not deleted
    And in "hidden_trash" there are "1" messages

  Scenario Outline: Delete messages for user with enabled admin_search 
    Given new initialized user with "$1, $2" in "trash"
    And he has enabled admin_search
    When we create "hidden_trash" folder "hidden_trash"
    And we move "$1" to "hidden_trash"
    And we delete "$1, $2"
    Then in "trash" there are "0" messages
    And "$1" is not deleted
    And "$2" is not deleted
    And in "hidden_trash" there are "2" messages
