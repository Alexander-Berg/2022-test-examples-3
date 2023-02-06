Feature: Directory sync
  Collie supports synchronization with directory

  Background:
    Given collie directory sync worker is started
    And org_id for new organization acquired

 Scenario: Process directory event
    When we expect stat request to cloud sharpei
    And we expect events request to directory which returns "events"
    And we expect org info request to directory
    And we expect users request to directory
    And we expect departments request to directory which returns "departments"
    And we expect groups request to directory
    And we add event to events queue
    And we wait until events queue is empty
    Then maildb contents is correct

  Scenario: Process directory event with empty email
    When we expect stat request to cloud sharpei
    And we expect events request to directory which returns "events"
    And we expect org info request to directory
    And we expect users request to directory
    And we expect departments request to directory which returns "department_with_empty_email"
    And we expect groups request to directory
    And we add event to events queue
    And we wait until events queue is empty
    Then empty email is not in maildb

  Scenario: Update contact with empty email
    When we create new organization
    And we expect stat request to cloud sharpei
    And we expect events request to directory which returns "events_for_updating_organization"
    And we expect org info request to directory
    And we expect users request to directory
    And we expect departments request to directory which returns "department_with_empty_email"
    And we expect groups request to directory
    And we add event to events queue
    And we wait until events queue is empty
    Then empty email is not in maildb

  Scenario: Delete organization
    When we create new organization
    And we expect stat request to cloud sharpei
    And we expect events request to directory which returns response with deleted organization
    And we add event to events queue
    And we wait until events queue is empty
    Then orgainzation has been deleted