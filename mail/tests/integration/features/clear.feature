Feature: Doberman should clear subscribed folder on subscription termination

    Background: Running doberman
       Given doberman acquired worker id "dobby"

    Scenario: Doberman deletes messages
       Given new user "bbs" in first shard with "inbox" shared folder
         And "5" messages in "inbox" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
        Then doberman put this subscription to "sync" state
        When I apply "unsubscription" action on this subscription
        Then doberman put this subscription to "terminated" state
         And in "Efim" folder "bbs" there are no messages

    Scenario: Doberman deletes only synced messages
       Given new user "bbs" in first shard with "inbox" shared folder
         And "5" messages in "inbox" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
         And I store user message into "bbs" at "Efim"
        Then doberman put this subscription to "sync" state
        When I apply "unsubscription" action on this subscription
        Then doberman put this subscription to "terminated" state
         And in "Efim" folder "bbs" there are "1" message

