Feature: Create list

  Scenario: Request endpoint /create_list via http post
    Given nothing
    When we request "create_list" as post
    Then response code is 405

  Scenario: Request endpoint /create_list with invalid uid
    Given invalid uid as "uid" in request
    When we request "create_list" with params
      | param                | value |
      | shared_folder_name   | bbs   |
    Then response code is 500
    And response has "error" with value containing "sharpei_client"

  Scenario: Request endpoint /create_list with valid params
    Given test user
    When we request "create_list" with params
      | param                | value |
      | shared_folder_name   | bbs   |
    Then response code is 200
    And response has "shared_folder_fid" with numeric value
    And folder was added to shared_folders table in db with name "bbs"
    And user has archivation rule

  Scenario: Test endpoint /create_list idempotency
    Given test user
    And he has a created list with name "bbs"
    When we request "create_list" with same params
    Then response code is 200
    And response has "shared_folder_fid" with numeric value
    And shared folder fid is same