Feature: Events
  Collie supports directory event adding for user present in db

  Background:
    Given collie is started
    And collie response to ping
    And org_id for new organization acquired
    And TVM2 service tickets for collie
    And TVM2 user ticket

  Scenario: Add directory event
    When we expect stat request to cloud sharpei
    And we request collie to add directory event
    Then response is ok
    And response is verified by json schema "status.json"
