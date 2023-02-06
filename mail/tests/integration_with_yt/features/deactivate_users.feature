Feature: deactivate_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are table in YT with passport active users

  Scenario: deactivate_users request should work
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call deactivate_users for user without activity in passport within last 2 years
    Given new user "Damian"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Damian" is in "inactive" state

  Scenario: Call deactivate_users for user with activity in passport within last 2 years
    Given new user "Dan"
    And he is in "active" state "732" days
    And he has activity in passport last 2 years
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Dan" is in "active" state

  Scenario: Call deactivate_users before default state_ttl(731 days)
    Given new user "Damon"
    And he is in "active" state "730" days
    And he has not activity in passport last 2 years
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Damon" is in "active" state

  Scenario: Call deactivate_users for pdd user
    Given new user "Dennis" with uid "1130000000000001"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Dennis" is in "active" state

  Scenario: Call deactivate_users for mailish user
    Given new user "Darcy"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    And he has mailish account entry
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Darcy" is in "active" state

  Scenario: Call deactivate_users for pro user
    Given new user "Domenic"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    And "Domenic" has setting "has_pro_interface" with value "on"
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Domenic" is in "active" state

  Scenario: Call deactivate_users for paid user
    Given new user "Douglas"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    And "Douglas" has setting "is_ad_disabled_via_billing" with value "on"
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Douglas" is in "active" state

  Scenario Outline: Call deactivate_users for user with filters
    Given new user "<user>"
    And he is in "active" state "732" days
    And he has not activity in passport last 2 years
    And "<user>" has enabled filter "<filter_type>"
    When we make deactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" is in "active" state

    Examples:
    | user   | filter_type       |
    | Dougal | forward           |
    | Duncan | forwardwithstore  |
    | Dwain  | reply             |
    | Dylan  | notify            |
