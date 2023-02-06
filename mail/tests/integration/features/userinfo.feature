Feature: Test hound response to /userinfo

  Scenario: v2/freezing_info returns user freezing info
    Given test user
    And user state is <state>, notifications count is <ncount>, last updated time is <lutime>
    When we request "v2/freezing_info"
    Then response is OK
    And state is <state>, notifications count is <ncount>, last update time is <lutime>

    Examples:
    | state    | ncount | lutime     |
    | active   | 3      | 2020-10-18 |
    | inactive | 2      | 2020-10-19 |
    | notified | 5      | 2020-10-15 |
    | frozen   | 1      | 2020-10-14 |
    | archived | 4      | 2020-10-16 |
    | deleted  | 7      | 2020-10-13 |
    | special  | 6      | 2020-10-17 |


  Scenario: v2/freezing_info returns 400 for nonexistent user
    Given nonexistent uid
    When we request "v2/freezing_info"
    Then response status is 400
    And there is error in response with "7001" code


  Scenario: v2/freezing_info returns 400 being called without args
    When we request "v2/freezing_info" without any argument
    Then response status is 400
    And there is error in response with "5001" code


  Scenario: v2/freezing_info returns 400 being called with invalid uid
    When we request "v2/freezing_info" with an invalid uid
    Then response status is 400
    And there is error in response with "5001" code
