Feature: Test hound response to /v2/unfreeze_user

  Scenario: v2/unfreeze_user returns 400 for nonexistent user
    Given nonexistent uid
    When we request "v2/unfreeze_user"
    Then response status is 400
    And there is error in response with "7001" code
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user returns 400 being called without args
    When we request "v2/unfreeze_user" without any argument
    Then response status is 400
    And there is error in response with "5001" code
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user returns 400 being called with invalid uid
    When we request "v2/unfreeze_user" with an invalid uid
    Then response status is 400
    And there is error in response with "5001" code
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user returns 400 for archived user
    Given test user
    And user state is "archived"
    When we request "v2/unfreeze_user"
    Then response status is 400
    And there is error in response with "5001" code
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user for frozen user
    Given test user
    And user state is "frozen"
    And passport will respond without errors
    When we request "v2/unfreeze_user"
    Then response is OK
    And user state is "active"
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user for frozen user when passport responds 500
    Given test user
    And user state is "frozen"
    And passport will respond with 500 2 times
    When we request "v2/unfreeze_user"
    Then response status is 500
    And user state is "frozen"
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user for frozen user when passport responds 400
    Given test user
    And user state is "frozen"
    And passport will respond with 400 1 times
    When we request "v2/unfreeze_user"
    Then response status is 400
    And user state is "frozen"
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user for frozen user when passport responds retriable errors
    Given test user
    And user state is "frozen"
    And passport will respond with retriable errors 2 times
    When we request "v2/unfreeze_user"
    Then response status is 500
    And user state is "frozen"
    And there are no unexpected requests to passport


  Scenario: v2/unfreeze_user for frozen user when passport responds illformed response
    Given test user
    And user state is "frozen"
    And passport will respond with illformed response 1 times
    When we request "v2/unfreeze_user"
    Then response status is 400
    And user state is "frozen"
    And there are no unexpected requests to passport
