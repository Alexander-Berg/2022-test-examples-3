Feature: Check task conflicts resolving in husky

  Scenario: Transfer waits for apply_data_migrations
    Given new user
    When we plan "apply_data_migrations" on sleeping husky "20001"
    And mark current task as "$apply"
    And we plan transfer
    And mark current task as "$transfer"
    And wait for husky to poll tasks
    Then "$transfer" status is "pending"
    When we move "$apply" to running husky
    Then "$transfer" was processed later than "$apply"
