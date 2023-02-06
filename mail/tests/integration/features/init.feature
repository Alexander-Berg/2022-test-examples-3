Feature: Doberman should copy messages when initialize subscription

    Background: Running doberman
       Given doberman acquired worker id "dobby"

    Scenario: Doberman copy messages
       Given new user "mail" in first shard with "inbox" shared folder
         And new user "Egor" in second shard
        When I store "5" messages into "inbox" at "mail"
         And I subscribe "mail" at "Egor" to "inbox" at "mail"
        Then doberman put this subscription to "sync" state
         And in "Egor" folder "mail" there are "5" messages
         And synced revision of "mail" at "Egor" is equal to "inbox" at "mail" revision

    Scenario: Doberman suppress notifications for storing message
       Given new user "mail" in first shard with "inbox" shared folder
         And new user "Egor" in second shard
        When I store "1" messages into "inbox" at "mail"
         And I subscribe "mail" at "Egor" to "inbox" at "mail"
        Then doberman put this subscription to "sync" state
         And "sync-store" is last changelog entry for "Egor" with suppressed notification

    Scenario: Doberman resumes copy messages for interrupted init
       Given new user "mail" in first shard with "inbox" shared folder
         And new user "Egor" in second shard
        When I store "5" messages into "inbox" at "mail"
         And I subscribe "mail" at "Egor" to "inbox" at "mail"
        When I interrupt synchronization
         And I delete one message from "inbox" at "mail"
         And I store "5" messages into "inbox" at "mail"
        When I start doberman
        Then doberman put this subscription to "sync" state
         And doberman apply this change
         And in "Egor" folder "mail" there are "9" messages
         And synced revision of "mail" at "Egor" is equal to "inbox" at "mail" revision
