Feature: Test hound for hidden_trash

  Scenario: call v2/folders when hidden_trash exists without with_hidden param
    Given test user
    And user has folder "hidden_trash" with symbol "hidden_trash"
    When we request "v2/folders"
    Then response is OK
    And there are no folder "hidden_trash" in response

  Scenario: call v2/folders when hidden_trash exists with with_hidden param
    Given test user
    And user has folder "hidden_trash" with symbol "hidden_trash"
    When we request "v2/folders" with args:
      | arg_name    | arg_value |
      | with_hidden |           |
    Then response is OK
    And there are folder "hidden_trash" in response

  Scenario: call folders when hidden_trash exists without with_hidden param
    Given test user
    And user has folder "hidden_trash" with symbol "hidden_trash"
    When we request "folders"
    Then response is OK
    And there are no folder "hidden_trash" in response

  Scenario: call folders when hidden_trash exists with with_hidden param
    Given test user
    And user has folder "hidden_trash" with symbol "hidden_trash"
    When we request "folders" with args:
      | arg_name    | arg_value |
      | with_hidden |           |
    Then response is OK
    And there are folder "hidden_trash" in response

  # MAILPG-4149
  Scenario: call messages_by_folder for hidden_trash folder
    Given test user
    And user has folder "hidden_trash" with symbol "hidden_trash" and fid "$fid"
    And in "hidden_trash" there are "3" messages
    When we request "messages_by_folder" for "$fid" folder
    Then response is OK
    And there are "3" messages in response
