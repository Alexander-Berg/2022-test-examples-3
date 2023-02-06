Feature: Shared contacts
  Collie supports shared contacts

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And org_id for new organization acquired
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Get user contacts with shared contacts if organization exists
    When we create contacts connect organization user
    And we prepare contacts for organization
    And we create contacts for organization
    And we prepare contacts for passport user
    And we request collie to create contacts
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get last created contacts with shared contacts
    Then response is ok
    And response has contacts with shared contacts

  Scenario: Get user contacts with shared contacts if organization not exists
    When we prepare contacts for passport user
    And we request collie to create contacts
    And we request collie to get last created contacts with shared contacts
    Then response is ok
    And response has contacts without shared contacts

  Scenario: Get only shared contacts if organization exists
    When we create contacts connect organization user
    And we prepare contacts for organization
    And we create contacts for organization
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get only shared contacts
    Then response is ok
    And response has only shared contacts

  Scenario: Get user contacts count with shared contacts
    When we create contacts connect organization user
    And we prepare contacts for organization
    And we create contacts for organization
    And we prepare contacts for passport user
    And we request collie to create contacts
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to get contacts count with shared contacts
    Then response is ok
    And response has contacts count with shared contacts

  Scenario: Get contacts via searchContacts with shared contacts
    When we create contacts connect organization user
    And we prepare contacts for organization
    And we create contacts for organization
    And we create emails for last created contact for organization
    And we prepare contacts for passport user
    And we request collie to create contacts
    And we share organization contacts with user and subscribe user to organization contacts
    And we request collie to search contacts with mixin_group
    Then response is ok
    And response has contact emails with shared contacts as ungrouped result
