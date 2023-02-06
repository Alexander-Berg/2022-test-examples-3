Feature: Modify users.state

  Scenario: New user has new state
    Given new initialized user
    Then user has "active" state

  Scenario Outline: Valid transitions of user states
    Given new initialized user
    And user has "<old_state>" state
    When we update user state to "<new_state>"
    Then user successfully changes state to "<new_state>"

    Examples:
      | old_state       | new_state    |
      | special         | active       |
      | special         | deleted      |
      | active          | special      |
      | active          | inactive     |
      | active          | deleted      |
      | inactive        | special      |
      | inactive        | active       |
      | inactive        | notified     |
      | inactive        | deleted      |
      | notified        | special      |
      | notified        | active       |
      | notified        | notified     |
      | notified        | frozen       |
      | notified        | deleted      |
      | frozen          | active       |
      | frozen          | archived     |
      | frozen          | deleted      |
      | archived        | active       |
      | archived        | deleted      |
      | deleted         | active       |


  Scenario Outline: Invalid transitions of user states
    Given new initialized user
    And user has "<old_state>" state
    When we try update user state to "<new_state>" as "$op" 
    Then commit "$op" should produce "InvalidUserStateTransition"

    Examples:
      | old_state      | new_state    |
      | special        | special      |
      | special        | inactive     |
      | special        | notified     |
      | special        | frozen       |
      | special        | archived     |
      | active         | active       |
      | active         | notified     |
      | active         | frozen       |
      | active         | archived     |
      | inactive       | inactive     |
      | inactive       | frozen       |
      | inactive       | archived     |
      | notified       | inactive     |
      | notified       | archived     |
      | frozen         | special      |
      | frozen         | inactive     |
      | frozen         | notified     |
      | frozen         | frozen       |
      | archived       | special      |
      | archived       | inactive     |
      | archived       | notified     |
      | archived       | frozen       |
      | archived       | archived     |
      | deleted        | special      |
      | deleted        | inactive     |
      | deleted        | notified     |
      | deleted        | frozen       |
      | deleted        | archived     |
      | deleted        | deleted      |


  Scenario: Change state for uninitialized user
    Given new user
    When we try update user state to "inactive" as "$op" 
    Then commit "$op" should produce "NotExistedUser"

  Scenario: Change state writes to changelog
    Given new initialized user
    When we update user state to "inactive" 
    Then in changelog there are
      | revision | type              |
      | 2        | user-state-update |
