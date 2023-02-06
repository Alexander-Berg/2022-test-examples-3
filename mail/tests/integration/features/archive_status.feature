Feature: Test hound response to v2/archive_status

  Scenario: v2/archive_status returns only user state for non-archived user
    Given test user
    And user state is "<user_state>"
    And archive state is "archivation_in_progress"
    When we request "v2/archive_status"
    Then response is OK
    And archive status info contains only user state, which is "<user_state>"

    Examples:
    | user_state |
    | inactive   |
    | notified   |
    | frozen     |
    | deleted    |
    | special    |
  

  Scenario: v2/archive_status returns full archive status info for archived user
    Given test user
    And user state is "<user_state>"
    When we request "v2/archive_status"
    Then response is OK
    And archive status info contains only user state, which is "<user_state>"
    Given message count is "<message_count>", restored message count is "<restored_message_count>", archive state is "<archive_state>"
    When we request "v2/archive_status"
    Then response is OK
    And archive status info is full: user state: "<user_state>", archive state: "<archive_state>", message count: "<message_count>", restored message count: "<restored_message_count>"

    Examples:
    | user_state | archive_state           | message_count | restored_message_count |
    | active     | archivation_in_progress | 59119         | 2165                   |
    | active     | archivation_complete    | 21896         | 6955                   |
    | active     | archivation_error       | 7074          | 3246                   |
    | active     | restoration_in_progress | 54041         | 17273                  |
    | active     | restoration_complete    | 68882         | 48268                  |
    | active     | restoration_error       | 29954         | 26556                  |
    | archived   | archivation_in_progress | 22976         | 12503                  |
    | archived   | archivation_complete    | 81203         | 66398                  |
    | archived   | archivation_error       | 11908         | 8407                   |
    | archived   | restoration_in_progress | 49850         | 37778                  |
    | archived   | restoration_complete    | 13617         | 7228                   |
    | archived   | restoration_error       | 29613         | 9444                   |
  

  Scenario: v2/archive_status not fails to response for user with state "archived" but without any archive info
    Given test user
    And user state is "archived"
    When we request "v2/archive_status"
    Then response is OK
    And archive status info contains only user state, which is "archived"


  Scenario: v2/archive_status returns 400 for nonexistent user
    Given nonexistent uid
    When we request "v2/archive_status"
    Then response status is 400
    And there is error in response with "7001" code


  Scenario: v2/archive_status returns 400 being called without args
    When we request "v2/archive_status" without any argument
    Then response status is 400
    And there is error in response with "5001" code


  Scenario: v2/archive_status returns 400 being called with invalid uid
    When we request "v2/archive_status" with an invalid uid
    Then response status is 400
    And there is error in response with "5001" code
