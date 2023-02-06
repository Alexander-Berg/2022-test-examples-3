Feature: Basic integration tests on husky

  @MAILPG-875
  Scenario: Plan user without mail sid cause such users does not have suid and db
    Given new user without mail suid and db
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2"
      }
      """
    Then task status is "error"
    And there was 1 tries
    And error is "no_such_user"
    And try notices contains "Not a mail user"

  Scenario: Husky moves successfully completed task to transfer.processed_tasks
    Given new user
    When we plan transfer
    Then task is successful
    And there is our task in processed_tasks
    And there is no our task in users_in_dogsleds

  Scenario: Husky leaves failed task in transfer.users_in_dogsleds
    Given new user absent in blackbox
    When we plan transfer
    Then task status is "error"
    And there is our task in users_in_dogsleds
    And there is no our task in processed_tasks

  Scenario: Test delay and retry for task with retryable error
    Given new user
    When we plan transfer
    Then task is successful
    When we make new user "RetryableError"
    When we plan "transfer"
      """
      {
        "from_db": "1",
        "to_db": "1"
      }
      """
    And wait "1" seconds for husky to poll tasks
    And we get task info
    Then status is "pending"
    And there was 1 tries
    And task planned in future
    When wait "3" seconds for husky to poll tasks
    And we get task info
    Then status is "error"
    And there was 3 tries
