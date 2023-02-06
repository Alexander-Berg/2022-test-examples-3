Feature: Test hound for case when new mail.folder_types created, but hound does not know it

  Scenario: call v2/folders when new folder type exists
    Given test user
    When we request "v2/folders"
    Then response is OK
    And there is no folder with empty type in response

    Given there is new symbol "brand_new_type"
    And user has folder "brand new folder" with symbol "brand_new_type"
    When we request "v2/folders"
    Then response is OK
    And there is folder with empty type in response
