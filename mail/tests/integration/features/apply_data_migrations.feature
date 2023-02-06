Feature: "Apply data migrations" task tests

  @MAILDEV-450
  Scenario: Plan apply_data_migrations
    Given new user
    When we plan "apply_data_migrations"
    Then task is successful

  @MAILDEV-450
  Scenario: Plan apply_data_migrations for user from stoplist
    Given new user in stoplist
    When we plan "apply_data_migrations"
    Then task is successful
