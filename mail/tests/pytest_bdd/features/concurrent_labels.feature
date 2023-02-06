Feature: Create new labels in different sessions

  Background: New user with 2 sessions
    Given new initialized user

  Scenario: Concurrent "jira" create
    When we try create "domain" label "jira" as "$first"
    And we try create "domain" label "jira" as "$second"
    When we rollback "$first"
    Then "domain" label "jira" does not exist
    When we commit "$second"
    Then "domain" label "jira" exists

  Scenario: 2 different labels concurrent create
    When we try create "domain" label "jira" as "$first"
    And we try create "user" label "OMG" as "$second"
    And we commit "$first"
    And we commit "$second"
    Then "domain" label "jira" exists
    And "user" label "OMG" exists
