Feature: Set archivation rule

  Scenario: Request endpoint /set_archivation_rule via http post
    Given nothing
    When we request "set_archivation_rule" as post
    Then response code is 405

  Scenario: Request endpoint /set_archivation_rule with invalid uid
    Given invalid uid as "uid" in request
    When we request "set_archivation_rule" with params
      | param     | value   |
      | type      | archive |
      | keep_days | 30      |
    Then response code is 500
    And response has "error" with value containing "sharpei_client"

  Scenario Outline: Request endpoint /set_archivation_rule with params
    Given test user with shared folder
    When we request "set_archivation_rule" with params
      | param     | value  |
      | type      | <type> |
      | keep_days | 30     |
      | max_size  | 1000   |
    Then response code is 200
    And response is ok
    And user has archivation rule
      | archive_type | keep_days | max_size |
      | <type>       | 30        | 1000     |
    Examples:
      | type    |
      | archive |
      | clean   |

  Scenario: Request endpoint /set_archivation_rule with unknown type
    Given test user with shared folder
    When we request "set_archivation_rule" with params
      | param     | value        |
      | type      | annihilation |
      | keep_days | 30           |
    Then response code is 400
    And response has "error" with value containing "unknown archivation type"

  Scenario: Request endpoint /set_archivation_rule with not shared folder
    Given test user with shared folder
    When we request "set_archivation_rule" with different shared_folder_fid and params
      | param     | value   |
      | type      | archive |
      | keep_days | 30      |
    Then response code is 400
    And response has "error" with value containing "folder is not shared or does not exist"

  Scenario: Test endpoint /set_archivation_rule idempotency
    Given test user with shared folder
    And user has archivation rule
    When we request "set_archivation_rule" with same params
    Then response code is 200
    And response is ok
    And user has archivation rule

  Scenario: Request endpoint /set_archivation_rule with different type
    Given test user with shared folder
    And user has archivation rule "archive"
    When we request "set_archivation_rule" with params
      | param     | value |
      | type      | clean |
      | keep_days | 30    |
      | max_size  | 1000  |
    Then response code is 200
    And response is ok
    And user has archivation rule
      | archive_type | keep_days | max_size |
      | clean        | 30        | 1000     |
