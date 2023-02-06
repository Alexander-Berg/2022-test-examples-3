Feature: Emails
  Collie supports email requesting for contacts in db

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Get emails
    When we request collie to get emails
    Then response is ok
    And response is verified by json schema "get_emails_result.json"
