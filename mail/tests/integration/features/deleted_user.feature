Feature: Test hound with deleted users

  Scenario: call enpoints for generic user
    Given test user with "3" deleted messages
    When we request "folders"
    Then response is OK
    When we request "deleted_messages"
    Then there are "3" deleted messages in response

  Scenario: call enpoints for non-existing user
    Given nonexistent uid
    When we request "folders"
    Then there is error in response with "7001" code
    When we request "deleted_messages"
    Then there is error in response with "7001" code

  Scenario: call enpoints for deleted user
    Given test user with "3" deleted messages
    And user was deleted
    When we request "folders"
    Then there is error in response with "7001" code
    When we request "deleted_messages"
    Then there are "3" deleted messages in response
