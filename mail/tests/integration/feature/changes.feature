Feature: Changes
  Collie supports changes reading from db

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Get changes
    When we prepare contact without emails
    And we request collie to create contacts
    And we request collie to get changes
    Then response is ok
    And response is verified by json schema "changes.json"

  Scenario: Restore
    When we request collie to restore to first revision
    Then response is ok
    And response is verified by json schema "status.json"
