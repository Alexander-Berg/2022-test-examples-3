Feature: Add directory event
  Addition of directory event in collie_db

  Background:
    Given collie is started
    And collie response to ping
    And org_id for new organization acquired
    And new organization user created

  Scenario: Add new event
    When we add directory event with default params
    Then we check event with default params in queue
    And we delete event from queue
  
  Scenario: Update event with larger event_revision
    When we add directory event with default params
    And we add directory event with "organization_deleted" type and revision "1"
    Then we check event with "organization_deleted" type and revision "1" in queue
    And we delete event from queue
  
  Scenario: Don't update event with lower event_revision 
    When we add directory event with "organization_updated" type and revision "3"
    And we add directory event with "organization_deleted" type and revision "2"
    Then we check event with "organization_updated" type and revision "3" in queue
    And we delete event from queue

  Scenario: Add organization event without event_revision
    When we add directory event with default params
    And we reset directory event revision to null
    And we add directory event with "organization_deleted" type and revision "2"
    Then we check event with "organization_deleted" type and revision "2" in queue
    And we delete event from queue
