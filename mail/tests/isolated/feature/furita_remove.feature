Feature: Checking rules removing functionality

    Background:
        Given furita is up and running

    Scenario: Calling remove rule method for the existent rule
        Given new user "Willow"

        When we create a rule "just test rule" for the user "Willow"
        Then user "Willow" has 1 rule

        When we remove rule "just test rule" of the user "Willow"
        Then furita replies with 200
        And user "Willow" has 0 rules

    Scenario: Calling remove rule method for many existent rules
        Given new user "Madmartigan"

        When we create a rule "first rule" for the user "Madmartigan"
        And we create a rule "second rule" for the user "Madmartigan"
        And we create a rule "third rule" for the user "Madmartigan"
        Then user "Madmartigan" has 3 rules

        When we remove the following rules of the user "Madmartigan":
            "first rule"
            "second rule"
        Then furita replies with 200
        And user "Madmartigan" has 1 rule

    Scenario: Calling remove rule method with empty id argument
        Given new user "Elora"

        When we remove rule "" of the user "Elora"
        Then furita replies with 400

    Scenario: Calling remove rule method without id argument
        Given new user "Bavmorda"

        When we remove rule "no rule with such name" of the user "Bavmorda"
        Then furita replies with 400
