Feature: freeze_users via shiva for surveillance users

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard

  Scenario: Call freeze_users for surveillance user
    Given new user "BorisSurveillance"
    And he is in "notified" state "11" days and has "2" notifies
    And surveillance will respond with "BorisSurveillance" uid in response  
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "BorisSurveillance" is in "special" state
    And there are no unexpected requests to passport

  Scenario: Call notify_users for surveillance user
    Given new user "GareySurveillance"
    And he is in "notified" state "2" days and has "0" notifies
    And surveillance will respond with "GareySurveillance" uid in response  
    When we make notify_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "GareySurveillance" is in "special" state

  Scenario: Call freeze_users when surveillance responds 500
    Given new user "BorisSurveillance500"
    And he is in "notified" state "11" days and has "2" notifies
    And surveillance will respond with 500 5 times
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "BorisSurveillance500" is in "notified" state
    And there are no unexpected requests to passport

  Scenario: Call freeze_users when monitoring responds with invalid data
    Given new user "BorisSurveillanceInvalid"
    And he is in "notified" state "11" days and has "2" notifies
    And surveillance will respond with invalid data
    When we make freeze_users request
    Then shiva responds ok
    And all shiva tasks finished
    And "BorisSurveillanceInvalid" is in "notified" state
    And there are no unexpected requests to passport
