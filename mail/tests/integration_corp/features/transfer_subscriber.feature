Feature: pg to pg transfer subscriber

#    Background: Running dobermans on both shards
#        Given working dobermans on both shards

    Scenario: Transfer subscriber
        Given shared folder "bbs" in first shard with "2" messages
        And new user "Efim" in first shard subscribed to "bbs"
        When I transfer "Efim" to second shard
        Then "Efim" leave in second shard
        And "Efim" marked as not here in first shard
        And "Efim" marked as here in second shard
        And "Efim" metadata is identical

    Scenario: Dobby sync changes after subscriber transfers
        Given shared folder "mail" in first shard with "3" messages
        And new user "Egor" in first shard subscribed to "mail"
        When I transfer "Egor" to second shard
        And I store "2" messages into shared folder "mail"
        Then doberman apply this changes
        And in "Egor" folder "mail" there are "5" messages
