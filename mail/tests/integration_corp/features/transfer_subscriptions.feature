Feature: pg to pg transfer subscriptions

#    Background: Running dobermans on both shards
#        Given working dobermans on both shards

    Scenario: Transfer subscriptions
        Given shared folder "sport" in first shard with "2" messages
        And folder "sport" has "clean" archivation rule with "30" days ttl
        And new user "Jone" in first shard subscribed to "sport"
        When I transfer "sport" to second shard
        Then "sport" leave in second shard
        And "sport" marked as not here in first shard
        And "sport" marked as here in second shard
        And "sport" metadata is identical except subscriptions state, worker_id and updated

    Scenario: Dobby sync changes after subscriptions transfers
        Given shared folder "staff" in first shard with "2" messages
        And new user "Jim" in first shard subscribed to "staff"
        When I transfer "staff" to second shard
        And I store "2" messages into shared folder "staff"
        Then doberman apply this changes
        And in "Jim" folder "staff" there are "4" messages

    Scenario: Transfer terminated subscriptions
        Given shared folder "football" in first shard with "2" messages
        And new user "Jade" in first shard with terminated subscription to "football"
        When I transfer "football" to second shard
        Then "football" leave in second shard
        And "football" marked as not here in first shard
        And "football" marked as here in second shard
        And "football" metadata is identical except subscriptions
        And there is no subscription now

    Scenario: Transfer failed subscriptions
        Given shared folder "tennis" in first shard with "2" messages
        And new user "Jake" in first shard with failed subscription to "tennis"
        Then transfer "tennis" to second shard failed with BadSubscriptionError
