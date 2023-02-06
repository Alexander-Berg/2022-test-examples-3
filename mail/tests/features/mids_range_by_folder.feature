Feature: Mids resolving with chunking
    Scenario: Resolve many mids with same received_date for purge
        Given test user with "11" messages
        And messages have received dates
            | received_date         |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2019-01-01 00:00:00" |
        When we request purge for fid "1"
        Then response is OK
        And wait 300 seconds for completion all async tasks
        And folder with fid "1" is empty

    Scenario: Resolve many mids with same received_date for label
        Given test user with "11" messages
        And messages have received dates
            | received_date         |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2020-01-01 00:00:00" |
            | "2019-01-01 00:00:00" |
        When we create label with name "label"
        Then response is OK
        When we request label fid "1" by lid from context
        Then response is OK
        And wait 300 seconds for completion all async tasks
        And all messages in folder with fid "1" has label
