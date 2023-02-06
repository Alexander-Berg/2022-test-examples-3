Feature: Checking list obtaining functionality 

    Background:
        Given furita is up and running

    Scenario: Obtaining the list of the rules
        Given new user "Turin"
        And user "Turin" has the following list of the rules:
            "First rule"
            "Second rule"
            "Third rule"

        When we obtain rules of the user "Turin"
        Then furita replies with 200
        And there are 3 rules in the obtained list

    Scenario: Obtaining the list of the rules with wrong type
        Given new user "Findus"
        And user "Findus" has the following list of the rules:
            "First rule"
            "Second rule"
            "Third rule"

        When we obtain rules with type "wrong one" of the user "Findus"
        Then furita replies with 400

    Scenario: Obtaining one rule by id without detalization
        Given new user "Radagast"
        And user "Radagast" has a rule "Just a rule"

        When we obtain the rule "Just a rule" of the user "Radagast"
        Then furita replies with 200
        And there is 1 rule in the obtained list
        And rule's "query" property is empty

    Scenario: Obtaining one rule by id with detalization
        Given new user "Kurufin"
        And user "Kurufin" has a rule "Just detailed rule"

        When we obtain detailed rule "Just detailed rule" of the user "Kurufin"
        Then furita replies with 200
        And there is 1 rule in the obtained list
        And rule's "query" property is filled

    Scenario: Obtaining the list of the rules without uid
        When we obtain rules of the user ""
        Then furita replies with 400

    Scenario: Obtaining the list of the rules with wrong uid (uid given but no user with such id)
        Given new user "InvalidUser"

        When we obtain rules of the user "InvalidUser"
        Then furita replies with 500
