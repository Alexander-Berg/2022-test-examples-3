Feature: Checking the ability to forbid the creating new forwarding and forwardwithstore rule if that is not allowed in the config

    Scenario: Trying to create new forwarding rule while such behaviour is not allowed in the config
        Given furita with allow_forward "none" is up and running
        And  new user "Leviathan"
        When create a "forward" rule for user "Leviathan"
        Then furita replies with 400
        And user "Leviathan" has 0 rules

    Scenario: Trying to create new forwardwithstore rule while such behaviour is not allowed in the config
        Given furita with allow_forward "none" is up and running
        And  new user "Baal"
        When create a "forwardwithstore" rule for user "Baal"
        Then furita replies with 400
        And user "Baal" has 0 rules

    Scenario: Creating notify rule while forward and forwardwithstore rules is not allowed in the config
        Given furita with allow_forward "none" is up and running
        And  new user "Mephisto"
        When create a "notify" rule for user "Mephisto"
        Then furita replies with 200
        And user "Mephisto" has 1 rule
    
    Scenario: Creating forwarding rule while such behaviour is allowed in the config
        Given furita with allow_forward "all" is up and running
        And  new user "Behemoth"
        When create a "forward" rule for user "Behemoth"
        Then furita replies with 200
        And user "Behemoth" has 1 rule
