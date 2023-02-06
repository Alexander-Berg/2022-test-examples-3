Feature: Concurrent initialization

  @no-check-violations
  Scenario: Concurrent registration the same user in different sessions
    Given new user
    When we initialize user as "$first"
    And we initialize user as "$second"
    And we commit "$second"
    Then "$second" result has one row
      | register_user       |
      | already_in_progress |
    When we commit "$first"
    Then user has just initialized "inbox"

  @allow-repeated-given
  Scenario: Concurrent registration different users in different sessions
    When we make user "Alice"
    And we initialize user as "$first"
    And we make user "Bob"
    And we initialize user as "$second"
    And we commit "$second"
    Then user has just initialized "inbox"
    And in changelog there is
      | revision | type     |
      | 1        | register |
    When "Alice" comeback
    And we commit "$first"
    Then user has just initialized "inbox"
    And in changelog there is
      | revision | type     |
      | 1        | register |
