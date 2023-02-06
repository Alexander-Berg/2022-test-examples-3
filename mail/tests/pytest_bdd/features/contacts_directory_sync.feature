Feature: Contacts synchronization from directory

  Scenario: Add contacts directory event
    Given new initialized user
    And new contacts "passport_user" user
    When we add contacts directory event with id "1"
    Then operation result is "success"
    And directory sync is enabled for contacts user
    And last directory event id is "1"
    And pending directory events count is "1"

  Scenario: Add outdated contacts directory event
    Given new initialized user
    And new contacts "passport_user" user
    And pending directory event with id "$e"
    When we add contacts directory event with id "$e"
    Then operation result is "outdated_event"

  Scenario: Complete sync contacts directory event
    Given new initialized user
    And new contacts "passport_user" user
    And pending directory event with id "$e"
    When we complete sync contacts directory event at revision "1" with id "$e"
    Then directory synced revision is "1"
    And last synced directory event id is "$e"
    And pending directory events count is "0"
