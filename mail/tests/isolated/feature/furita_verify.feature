Feature: Checking rules verification functionality

    Background:
        Given furita is up and running

    Scenario: Calling without verification code
        When we verify with code ""
        Then furita replies with 400

    Scenario: Calling with incorrect verification code
        When we verify with fake code
        Then furita replies with 400

    Scenario: Verifying rule with correct code
        Given new user "SomeUser01"
        And user "SomeUser01" has a rule "The Rule" forwarding to "SomeUser02@yandex.ru"

        When we verify with correct code for rule "The Rule" of user "SomeUser01" 1 times
        Then furita replies with 200
        And the rule "The Rule" of user "SomeUser01" is verified

    Scenario: Verifying the same rule twice
        Given new user "SomeUser03"
        And user "SomeUser03" has a rule "Another Rule" forwarding to "SomeUser04@yandex.ru"

        When we verify with correct code for rule "Another Rule" of user "SomeUser03" 2 times
        Then furita replies with 500
        And the rule "Another Rule" of user "SomeUser03" is verified
