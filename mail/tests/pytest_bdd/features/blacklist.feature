Feature: Filters blacklist

  Scenario: Initially blacklist is empty
    Given new initialized user
    Then blacklist is empty

  Scenario: Add email to blacklist
    Given new initialized user
    When we add "spam@devnull" to blacklist
    Then blacklist is
      | email        |
      | spam@devnull |

  Scenario: Add and remove email to blacklist
    Given new initialized user
    When we add emails to blacklist
      | email              |
      | spam@devnull       |
      | not-a-spam@devnull |
    Then blacklist is
      | email              |
      | spam@devnull       |
      | not-a-spam@devnull |
    When we remove "spam@devnull" from blacklist
    Then blacklist is
      | email              |
      | not-a-spam@devnull |
    When we remove "not-a-spam@devnull" from blacklist
    Then blacklist is empty

  Scenario: Blacklist has limit [actually 1000 emails, but 3 for tests]
    Given new initialized user
    When we add emails to blacklist
      | email         |
      | apple@fruits  |
      | banana@fruits |
      | peach@fruits  |
    Then blacklist is
      | email         |
      | apple@fruits  |
      | banana@fruits |
      | peach@fruits  |
    When we add "potato@vegetables" to blacklist
    Then blacklist is
      | email         |
      | apple@fruits  |
      | banana@fruits |
      | peach@fruits  |
    When we remove "apple@fruits" from blacklist
    And we add "plum@fruits" to blacklist
    Then blacklist is
      | email         |
      | banana@fruits |
      | peach@fruits  |
      | plum@fruits   |

  Scenario: Uninitialized user can't add to blacklist
    Given new user
    When we try add "foo.bar@baz" to blacklist as "$op"
    Then commit "$op" should produce "NotInitializedUserError"

  @other-user
  Scenario: Add and remove from blacklist do not touch other users
    Given new initialized user
    And replication stream
    When we add "spam@devnull" to blacklist
    And we remove "spam@devnull" from blacklist
    Then there are only our user changes in replication stream
