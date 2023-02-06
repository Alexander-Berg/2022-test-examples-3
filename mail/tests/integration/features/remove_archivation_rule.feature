Feature: Remove archivation rule

  Scenario: Request endpoint /remove_archivation_rule via http post
    Given nothing
    When we request "remove_archivation_rule" as post
    Then response code is 405

  Scenario: Request endpoint /remove_archivation_rule with invalid uid
    Given invalid uid as "uid" in request
    When we request "remove_archivation_rule"
    Then response code is 500
    And response has "error" with value containing "sharpei_client"

  Scenario: Request endpoint /remove_archivation_rule with params
    Given test user with shared folder
    And user has archivation rule
    When we request "remove_archivation_rule"
    Then response code is 200
    And response is ok
    And user has no archivation rules

  Scenario: Test endpoint /remove_archivation_rule idempotency
    Given test user with shared folder
    When we request "remove_archivation_rule"
    Then response code is 200
    And response is ok
    And user has no archivation rules
