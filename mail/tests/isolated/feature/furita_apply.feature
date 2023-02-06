Feature: Checking rules applying functionality 

    Background:
        Given furita is up and running

    Scenario: Calling without params
        When we call apply without uid
        Then furita replies with 400

    Scenario: Applying nonexistent rule
        Given new user "Hurin"

        When we try to apply rule "" of the user "Hurin"
        Then furita replies with 200

    Scenario: Applying existing rule
        Given new user "Haldad"
        And user "Haldad" has a rule "The only rule"

        When we try to apply rule "The only rule" of the user "Haldad"
        Then furita replies with 200
