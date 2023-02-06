Feature: Doberman should sync changes

    Background: Running doberman
       Given doberman acquired worker id "dobby"

    Scenario: Doberman sync store change
       Given new user "bbs" in first shard with "inbox" shared folder
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
        Then doberman put this subscription to "sync" state
        When I store message into "inbox" at "bbs"
        Then doberman apply this change
         And in "Efim" folder "bbs" there is one message
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision


    Scenario: Doberman sync copy change
        . add this specific change,
        . cause COPY produce store with many messages
       Given new user "bbs" in first shard with "inbox" shared folder
         And "3" messages in "drafts" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
        Then doberman put this subscription to "sync" state
        When I copy all messages from "drafts" to "inbox" at "bbs"
        Then doberman apply this changes
         And in "Efim" folder "bbs" there are "3" messages
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision


    Scenario: Doberman sync delete change
       Given new user "bbs" in first shard with "inbox" shared folder
         And "2" messages in "inbox" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
        Then doberman put this subscription to "sync" state
        When I delete all messages from "inbox" at "bbs"
        Then doberman apply this change
         And in "Efim" folder "bbs" there are no messages
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision


    Scenario: Doberman sync update change for mark/unmark by label
       Given new user "bbs" in first shard with "inbox" shared folder
         And message in "inbox" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
        Then doberman put this subscription to "sync" state

        When I create "domain" label "StarTrek" at "bbs"
         And I "mark" all message from "inbox" at "bbs" by "domain" label "StarTrek"
        Then doberman apply this change
         And "Efim" has "domain" label "StarTrek"
         And in "Efim" folder "bbs" all messages has "domain" label "StarTrek"
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision

        When I "unmark" all message from "inbox" at "bbs" by "domain" label "StarTrek"
        Then doberman apply this change
         And in "Efim" folder "bbs" all messages has not "domain" label "StarTrek"
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision


    Scenario: Doberman sync update change for mark/unmark by fake label only for deleted
       Given new user "bbs" in first shard with "inbox" shared folder
         And message in "inbox" at "bbs"
         And all message from "inbox" at "bbs" are "unset" seen recent deleted
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
        Then doberman put this subscription to "sync" state

        When I "set" seen recent deleted all message from "inbox" at "bbs"
        Then doberman apply this change
         And in "Efim" folder "bbs" all messages have deleted label
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision

        When I "unset" seen recent deleted all message from "inbox" at "bbs"
        Then doberman apply this change
         And in "Efim" folder "bbs" all messages do not have deleted label
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision


    Scenario: Doberman sync join threads change
       Given new user "bbs" in first shard with "inbox" shared folder
         And "3" messages in "inbox" at "bbs"
         And new user "Efim" in second shard subscribed to "inbox" at "bbs"
         And all messages from "inbox" at "bbs" synced to "Efim"
        Then doberman put this subscription to "sync" state
        When I join all threads in one from "inbox" at "bbs"
        Then doberman apply this change
         And in "Efim" folder "bbs" all messages has the same thread
         And synced revision of "bbs" at "Efim" is equal to "inbox" at "bbs" revision
