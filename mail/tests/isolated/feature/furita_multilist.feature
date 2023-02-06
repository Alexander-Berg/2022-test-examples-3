Feature: Checking multilist obtaining functionality 

    Background:
        Given furita is up and running

    Scenario: Obtaining the multilist of the rules for one user
        Given new users: Teodor 
        
        Then user "Teodor" has the following list of the rules:
             "First rule"
             "Second rule"
             "Third rule"
        
        When we obtain rules for the following users: Teodor
        Then furita replies with 200
        And user "Teodor" have ok result with 3 rules

    Scenario: Obtaining the multilist of the rules for two users
        Given new users: Bragi,Lesli

        Then user "Bragi" has the following list of the rules:
             "First rule"
             "Second rule"
             "Third rule"

        Then user "Lesli" has the following list of the rules:
             "First rule"
             "Second rule"
 
        When we obtain rules for the following users: Bragi,Lesli
        
        Then furita replies with 200
        And user "Bragi" have ok result with 3 rules
        And user "Lesli" have ok result with 2 rules

    Scenario: Obtaining the multilist of the rules for three users
        Given new users: Ibragim,Paragrin,InvalidUser1

        Then user "Ibragim" has the following list of the rules:
             "First rule"
             "Second rule"
             "Third rule"

        When we obtain rules for the following users: Ibragim,Paragrin,InvalidUser1

        Then furita replies with 200
        And user "Ibragim" have ok result with 3 rules
        And user "Paragrin" have ok result with 0 rules
        And user "InvalidUser1" have error result with message "Sharpei service responded with 404"
