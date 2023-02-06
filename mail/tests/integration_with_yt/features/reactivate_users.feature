Feature: reactivate_users via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are table in YT with passport active users

  Scenario: reactivate_users request should work
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario Outline: Call reactivate_users for mailish user in "inactive" states and non-frozen in blackbox
    Given new user "<user>"
    And he is in "<state>" state "0" days
    And he has not activity in passport last 2 years
    And he has mailish account entry
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" is in "active" state

    Examples:
    | user     | state      |
    | Ebenezer | inactive   |
    | Edmund   | notified   |


  Scenario Outline: Call reactivate_users for mailish user in "frozen" states and frozen in blackbox 
    Given new user "<user>"
    And "<user>" frozen in blackbox
    And he is in "<state>" state "0" days
    And he has not activity in passport last 2 years
    And he has mailish account entry
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" is in "<state>" state

    Examples:
    | user           | state      |
    | EleazarFrozen  | frozen     |
    | EddyFrozen     | archived   |


  Scenario: Call reactivate_users for pdd user
    Given new user "Elvin" with uid "1130000000000002"
    And he is in "inactive" state "0" days
    And he has not activity in passport last 2 years
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Elvin" is in "active" state

  Scenario: Call reactivate_users for pro user
    Given new user "Ely"
    And he is in "inactive" state "0" days
    And he has not activity in passport last 2 years
    And "Ely" has setting "has_pro_interface" with value "on"
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Ely" is in "active" state

  Scenario: Call reactivate_users for paid user
    Given new user "Emery"
    And he is in "inactive" state "0" days
    And he has not activity in passport last 2 years
    And "Emery" has setting "is_ad_disabled_via_billing" with value "on"
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Emery" is in "active" state

  Scenario Outline: Call reactivate_users for user with filters
    Given new user "<user>"
    And he is in "inactive" state "0" days
    And he has not activity in passport last 2 years
    And "<user>" has enabled filter "<filter_type>"
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" is in "active" state

    Examples:
    | user   | filter_type       |
    | Erik   | forward           |
    | Ernest | forwardwithstore  |
    | Esdras | reply             |
    | Ethan  | notify            |


  Scenario: Call reactivate_users for general user with activity in passport within last 2 years
    Given new user "Emin"
    And he has activity in passport last 2 years
    And he is in "inactive" state "0" days
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Emin" is in "active" state

  Scenario: Call reactivate_users for general user without activity in passport within last 2 years
    Given new user "Emil"
    And he has not activity in passport last 2 years
    And he is in "inactive" state "0" days
    When we make reactivate_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "Emil" is in "inactive" state
