Feature: Lists
  Collie supports shared lists handlers in db

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And org_id for new organization acquired
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Get empty shared lists
    When we request collie to get shared lists
    Then response is ok
    And response is verified by json schema "existing_shared_lists.json"
    And response has empty lists

  Scenario: Get shared list
    When we create contacts connect organization user
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    Then response is ok
    And response is verified by json schema "existing_shared_lists.json"
    And response has one shared list

  Scenario: Get shared contact count from list
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get shared contact count from list
    Then response is ok
    And response is verified by json schema "list_contacts_counter.json"
    And response has shared contact count from list equal to "5"

  Scenario: Get shared contact with emails count from list
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get shared contact with emails count from list
    Then response is ok
    And response is verified by json schema "list_contacts_counter.json"
    And response has shared contact count from list equal to "3"

  Scenario: Get all shared contacts from list
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts from list
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts from list

  Scenario: Get all shared contacts with emails from list
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts with emails from list
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts with emails from list

Scenario: Get all shared contacts from list with offset and limit
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts from list with offset and limit
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts from list with offset and limit

Scenario: Get all shared contacts with emails from list with offset and limit
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts with emails from list with offset and limit
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts with emails from list with offset and limit

Scenario: Get all shared contacts from list by IDs with offset and limit
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts from list by IDs with offset and limit
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts from list by IDs with offset and limit

Scenario: Get all shared contacts with emails from list by IDs with offset and limit
    When we create contacts connect organization user
    And we prepare extended contacts for organization
    And we create contacts for organization
    And we create extended emails for last created contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get shared lists
    And we request collie to get all shared contacts with emails from list by IDs with offset and limit
    Then response is ok
    And response is verified by json schema "existing_contacts.json"
    And response has all shared contacts with emails from list by IDs with offset and limit
