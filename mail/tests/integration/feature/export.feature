Feature: Export
  Sheltie supports export user contacts

  Background:
    Given sheltie is started
    And sheltie response to ping

  Scenario: Export contacts to collie
    When we expect request collie to existing contacts
    And we request sheltie to export contacts
    Then response is "export_contacts_response"