Feature: Tabs features

  Background: New user without tabs
    Given new initialized user
    And user does not have tabs

  Scenario: get_or_create tab creates tab
    When we create tab "news"
    Then user has tab "news"

  Scenario: get_or_create tab returns created tab
    When we create tab "social" as "$created"
    Then user has tab "social" as "$found"
    And tabs "$created" and "$found" are equal

  Scenario: get_or_create tab returns existing tab
    When we create tab "relevant" as "$created"
    And we create tab "relevant" as "$existed"
    Then tabs "$created" and "$existed" are equal

  Scenario: get_or_create tab increments revision on create
    When we create tab "news"
    Then global revision is "2"
    When we create tab "social"
    Then global revision is "3"

  Scenario: get_or_create tab does not increment revision on get
    When we create tab "news"
    Then global revision is "2"
    When we create tab "news"
    Then global revision is "2"

  Scenario: get_or_create tab writes to changelog
    When we create tab "news"
    Then in changelog there is
      | revision | type       |
      | 2        | tab-create |

  @other-user
  Scenario: get_or_create tab does not touch other users
    Given replication stream
    When we create tab "news"
    Then there are only our user changes in replication stream


  Scenario: reset_unvisited on non-existing tab does nothing
    When we reset tab "news"
    Then user does not have tab "news"

  Scenario: reset_unvisited on visited tab does nothing
    When we create tab "social"
    Then tab "social" is not unvisited
    When we reset tab "social"
    Then tab "social" is not unvisited

  Scenario: reset_unvisited on unvisited tab modifies it
    When we create unvisited tab "relevant"
    Then tab "relevant" is unvisited
    When we reset tab "relevant"
    Then tab "relevant" is not unvisited

  Scenario: reset_unvisited increments revision on modify
    When we create unvisited tab "news"
    Then global revision is "2"
    When we reset tab "news"
    Then global revision is "3"

  Scenario: reset_unvisited does not increment revision on no-modify
    When we create tab "news"
    Then global revision is "2"
    When we reset tab "news"
    Then global revision is "2"

  Scenario: reset_unvisited writes to changelog
    When we create unvisited tab "news"
    And we reset tab "news"
    Then in changelog there is
      | revision | type                |
      | 3        | tab-reset-unvisited |


  Scenario: reset_fresh on non-existing tab does nothing
    When we reset fresh on tab "news"
    Then user does not have tab "news"

  Scenario: reset_fresh on tab without fresh does nothing
    When we create tab "social"
    Then tab "social" has no fresh
    And global revision is "2"
    When we reset fresh on tab "social"
    Then tab "social" has no fresh
    And global revision is "2"

  Scenario: reset_fresh on tab with fresh modifies it
    When we create tab "relevant" with "5" fresh
    Then global revision is "2"
    When we reset fresh on tab "relevant"
    Then tab "relevant" has no fresh
    And global revision is "3"
    And in changelog there is
      | revision | type                |
      | 3        | tab-reset-unvisited |

  @other-user
  Scenario: Create tab does not touch other users
    Given replication stream
    When we create unvisited tab "news"
    And we reset tab "news"
    Then there are only our user changes in replication stream
