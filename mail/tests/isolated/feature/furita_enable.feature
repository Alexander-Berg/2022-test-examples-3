Feature: Checking enabling and disabling functionality 

    Background:
        Given furita is up and running

    Scenario: Calling disable rule method for the existent rule in enabled state
        Given new user "Ubertin"

        When we create a rule "just test rule" for the user "Ubertin"
        Then user "Ubertin" has the rule "just test rule" enabled

        When user "Ubertin" switches off the rule "just test rule"
        Then furita replies with 200
        And user "Ubertin" has the rule "just test rule" disabled

    Scenario: Calling disable rule method with empty id argument
        Given new user "Corvinus"

        When user "Corvinus" switches off the rule ""
        Then furita replies with 400

    Scenario: Calling disable rule method without id argument
        Given new user "Bartholomeus"

        When user "Bartholomeus" switches off the rule "no rule with such name"
        Then furita replies with 400
