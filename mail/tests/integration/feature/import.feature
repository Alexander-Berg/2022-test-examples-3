Feature: Import
  Sheltie supports import user contacts

  Background:
    Given sheltie is started
    And sheltie response to ping

  Scenario: Import contacts to collie
    When we expect request collie to existing contacts
    And we expect request сollie to create contacts
    And we request sheltie to import contacts
    Then response is "import_contacts_response"
