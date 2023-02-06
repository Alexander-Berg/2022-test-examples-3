Feature: Doberman process subscriptions

    Background: Running doberman
       Given doberman is stopped
         And free worker id "dobby"

    Scenario: Doberman should take subscriptions assigned to him
       Given new user "staff" in first shard
         And "inbox" at "staff" shared folder
         And new user "Arkady" in second shard
         And "staff" at "Arkady" subscribed to "inbox" at "staff"
        When I assign this subscription to "dobby"
         And I start doberman
        Then doberman put this subscription to "sync" state

    Scenario: Doberman should take subscriptions which is assigned to no one
       Given doberman is running
         And new user "staff" in first shard with "inbox" shared folder
         And new user "Arkady" in second shard subscribed to "inbox" at "staff"
        Then our doberman assigned to this subscription
        Then doberman put this subscription to "sync" state

    Scenario: Doberman should process unsubscription
       Given doberman is running
         And new user "staff" in first shard with "inbox" shared folder
         And new user "Arkady" in second shard subscribed to "inbox" at "staff"
        Then doberman put this subscription to "sync" state
        When I apply "unsubscription" action on this subscription
        Then doberman put this subscription to "discontinued" state
