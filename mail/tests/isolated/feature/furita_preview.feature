Feature: Checking rules preview functionality 

    Background:
        Given furita is up and running

    Scenario: Calling with empty condition fields
        Given new user "Halmir"

        When we preview empty rule of the user "Halmir"
        Then furita replies with 400

    Scenario: Calling with some condition
        Given new user "Hareth"
        
        When we preview rule of user "Hareth" with params:
            field1=from
            field2=1
            field3=test@test.ru
        Then furita replies with 200

    Scenario: Calling with existing rule
        Given new user "Huor"
        And user "Huor" has a rule "Rule00"

        When we preview rule "Rule00" of user "Huor"
        Then furita replies with 200

    Scenario: Calling with nonexistent rule
        Given new user "Hador"

        When we preview rule "" of user "Hador"
        Then furita replies with 500
