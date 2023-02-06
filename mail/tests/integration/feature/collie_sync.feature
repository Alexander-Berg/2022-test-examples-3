Feature: Collie sync
  Collie supports synchronization with staff and ml

  Background:
    Given set ml and staff sync timestamp
    And create ml user
    And create staff user
    And new passport users for staff
    And collie sync is started

  Scenario: Collie updates only changed ml contacts
    When we expect stat request to cloud sharpei
    And we expect request to ml which returns new ml contacts
    And we expect request to ml which returns updated ml contacts
    Then we check revision of unchanged contacts

  Scenario: Collie sync staff contacts
    When we expect stat request to cloud sharpei
    And we expect request to staff which returns new staff contacts
    And we expect request to staff which returns updated staff contacts
    Then we check updated contacts
