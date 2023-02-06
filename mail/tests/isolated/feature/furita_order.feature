Feature: Checking rules ordering functionality

    Background:
        Given furita is up and running

    Scenario: Calling order rules method for the existent rules
        Given new user "Baleog"

        When we create the following list of the rules for the user "Baleog":
            "fourth rule"
            "third rule"
            "second rule"
            "first rule"
        Then user "Baleog" has the following ordered by priority list of the rules:
            "first rule"
            "second rule"
            "third rule"
            "fourth rule"

        When we order the rules of the user "Baleog" as follow:
            "second rule"
            "fourth rule"
            "third rule"
            "first rule"
        Then furita replies with 200
        And user "Baleog" has the following ordered by priority list of the rules:
            "second rule"
            "fourth rule"
            "third rule"
            "first rule"

    Scenario: Calling order rules method for incompleted list of the rules
        Given new user "Olaf"

        When we create the following list of the rules for the user "Olaf":
            "one"
            "two"
            "three"
            "four"
        And we order the rules of the user "Olaf" as follow:
            "one"
            "four"
        Then furita replies with 400

    Scenario: Calling order rules method with duplicated ids
        Given new user "Eric"

        When we create the following list of the rules for the user "Eric":
            "one"
            "two"
            "three"
        And we order the rules of the user "Eric" as follow:
            "one"
            "two"
            "three"
            "three"
        Then furita replies with 400

    Scenario: Calling order rules method without list of the rules
        Given new user "Swift"

        When we order the rules of the user "Swift" as follow:
            ""
        Then furita replies with 400

