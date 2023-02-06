Feature: Creating rules on the prestable environment

    Background:
        Given initialised <user> with <password> at <passport_host>, <wmi_host>, <mops_host>, <akita_host>, <sendbernar_host> with <x_original>

    Scenario Outline: Creating a couple of rules then obtaining them
        When we restart furita at <furita_host>
        Then furita at <furita_host> is up and running

        When we obtain all rules
        And remove all obtained rules
        And we obtain all rules
        Then there are no rules

        When we create a rule "just a rule"
        And we create a rule "another rule"
        And we obtain all rules
        Then there are 2 rules

    Scenario Outline: Applying a rule to the messages
        When we restart furita at <furita_host>
        Then furita at <furita_host> is up and running

        When we obtain all rules
        And remove all obtained rules
        And we obtain all rules
        Then there are no rules

        When we remove all envelopes from the folder "inbox"
        Then there are no envelopes in the folder "inbox"

        When we send mail to the <user> with the subject "AAA and some text"
        And we send mail to the <user> with the subject "AAA and another text"
        And we send mail to the <user> with the subject "BBB with just text"
        Then there are 3 envelopes in the folder "inbox"

        When we create the rule which founds "AAA" string in the subject with name "Delete AAA"
        And we obtain all rules
        Then there is 1 rule

        When we apply the rule "Delete AAA"
        And wait 5 seconds
        Then there is 1 envelope in the folder "inbox"

    Scenario Outline: Applying a rule with stars to the messages
        When we restart furita at <furita_host>
        Then furita at <furita_host> is up and running

        When we obtain all rules
        And remove all obtained rules
        And we obtain all rules
        Then there are no rules

        When we remove all envelopes from the folder "inbox"
        Then there are no envelopes in the folder "inbox"

        When we send mail to the <user> with the subject "** AAA and some text"
        And we send mail to the <user> with the subject "AAA and another text"
        And we send mail to the <user> with the subject "** BBB with just text"
        Then there are 3 envelopes in the folder "inbox"

        When we create the rule which founds "**" string in the subject with name "Delete stars"
        And we obtain all rules
        Then there is 1 rule

        When we apply the rule "Delete stars"
        And wait 5 seconds
        Then there is 1 envelope in the folder "inbox"
        And there is an envelope with the subject "AAA and another text" in the "inbox"

    Scenario Outline: Check how furita sends confirmation messages
        When we restart furita at <furita_host>
        Then furita at <furita_host> is up and running

        When we remove all envelopes from the folder "inbox"
        Then there are no envelopes in the folder "inbox"

        When we create selfnotification rule for the <user>
        And wait 5 seconds
        Then there is an envelope with the subject "Address confirmation for receiving notifications " in the "inbox"
