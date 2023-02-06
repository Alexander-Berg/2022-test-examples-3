Feature: Test hound returns tabs

  Scenario: v2/tabs for fresh user returns tabs
    Given test user
    When we request "v2/tabs"
    Then response is OK
    And there are "3" tabs in response
      | type     | messagesCount | unreadMessagesCount |
      | news     | 0             | 0                   |
      | relevant | 0             | 0                   |
      | social   | 0             | 0                   |


  Scenario: v2/tabs for user who cannot read tabs returns empty tabs list
    Given test user with "2" messages in tab "social"
    And user cannot read tabs
    When we request "v2/tabs"
    Then response is OK
    And there are "0" tabs in response


  Scenario: v2/tabs for user who can read tabs returns tabs
    Given test user with "2" messages in tab "news"
    And user can read tabs
    When we request "v2/tabs"
    Then response is OK
    And there are "3" tabs in response
      | type     | messagesCount | unreadMessagesCount |
      | news     | 2             | 1                   |
      | relevant | 0             | 0                   |
      | social   | 0             | 0                   |


  Scenario: folder_tabs_new_counters for user who cannot read tabs returns empty set
    Given test user with "2" messages in tab "social"
    And user cannot read tabs
    When we request "folder_tabs_new_counters"
    Then response is OK
    And "folder_tabs_new_counters" response is empty


  Scenario: folder_tabs_new_counters for user who can read tabs returns tabs
    Given test user with "2" messages in tab "relevant"
    And user can read tabs
    When we request "folder_tabs_new_counters"
    Then response is OK
    And "folder_tabs_new_counters" response with tabs
      | type     | unreadMessagesCount |
      | relevant | 1                   |
      | news     | 0                   |
      | social   | 0                   |

  Scenario: folder_tabs_new_counters does not return more than limit
    Given test user with "10" messages in tab "social"
    And user can read tabs
    When we request "v2/tabs"
    Then there are "3" tabs in response
      | type     | messagesCount | unreadMessagesCount |
      | relevant | 0             | 0                   |
      | news     | 0             | 0                   |
      | social   | 10            | 5                   |
    When we request "folder_tabs_new_counters" with "3" limit
    Then "folder_tabs_new_counters" response with tabs
      | type     | unreadMessagesCount |
      | relevant | 0                   |
      | news     | 0                   |
      | social   | 3                   |
