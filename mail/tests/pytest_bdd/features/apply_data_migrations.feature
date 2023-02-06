Feature: Test for util.apply_data_migrations.feature


  @MAILDEV-448
  Scenario: Call fix on broken user
    Given new initialized user
    And data version is set to minimum
    When we accidentally delete "system" label "pinned"
    Then "system" label "pinned" does not exist
    And check produce
      | name                 |
      | labels.missed_system |
    When we execute "apply_data_migrations" util
    Then data version is maximum
    And fix log contains
      | name                 |
      | labels.missed_system |
