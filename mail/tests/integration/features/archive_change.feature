Feature: Test hound response to /v2/archive_change
    Examples:
        | acts        | r_acts       |
        | restoration | cleaning    |
        | cleaning    | restoration |
    
    Background: All on the same endpoint
        Given endpoint is "/v2/archive_change"

    Scenario: returns 400 being called without args
        When we request without args
        Then response status is 400
        And there is error in response with "5001" code
        And there are no unexpected requests to passport


    Scenario: returns 400 being called with invalid archive action
        When we request with "action=lol_what"
        Then response status is 400
        And there is error in response with "5001" code
        And there are no unexpected requests to passport


    Scenario: returns 400 for non-archived user
        Given test user
        And "<r_acts>" is unused
        And user state is "<states>"
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "<states>"
        And there is error in response with "1000" code
        And there are no unexpected requests to passport
        Examples:
            | states   |
            | active   |
            | inactive |
            | frozen   |
            | deleted  |


    Scenario: returns 400 for user without an archive
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "archived"
        And there is error in response with "1000" code
        And there are no unexpected requests to passport


    Scenario: returns 400 for user with an archive in wrong state
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "<wrong_states>"
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "archived"
        And archive state is "<wrong_states>"
        And there are no unexpected requests to passport
        Examples:
            | wrong_states |
            | archivation_in_progress |
            | archivation_error |
            | restoration_in_progress |
            | restoration_error|
            | cleaning_in_progress |


    Scenario: returns 400 for user with an archive in requested state and opposite action
        Given test user
        And user state is "archived"
        And archive state is "<r_acts>_requested"
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "archived"
        And archive state is "<r_acts>_requested"
        And there are no unexpected requests to passport


    Scenario: when passport responds 500
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with 500 2 times
        When we request with "action=<acts>"
        Then response status is 500
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport


    Scenario: when passport responds 400
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with 400 1 times
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport


    Scenario: when passport responds retriable errors
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with retriable errors 2 times
        When we request with "action=<acts>"
        Then response status is 500
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport


    Scenario: when passport responds illformed response
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with illformed response 1 times
        When we request with "action=<acts>"
        Then response status is 400
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport


    # SUCCESS SCENARIOS


    Scenario: returns 200 for user with an archive
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond without errors
        When we request with "action=<acts>"
        Then response status is 200
        And user state is "active"
        And archive state is "<acts>_in_progress"
        And there are no unexpected requests to passport


    Scenario: returns 200 for user with an archive with retry
        Given test user
        And "<r_acts>" is unused
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with retriable errors 2 times
        When we request with "action=<acts>"
        Then response status is 500
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport

        Given passport will respond without errors
        When we request with "action=<acts>"
        Then response status is 200
        And user state is "active"
        And archive state is "<acts>_in_progress"
        And there are no unexpected requests to passport


    Scenario: returns 200 for user with an archive with 1 bad retry
        Given test user
        And user state is "archived"
        And archive state is "archivation_complete"
        And passport will respond with retriable errors 2 times
        When we request with "action=<acts>"
        Then response status is 500
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport


        When we request with "action=<r_acts>"
        Then response status is 400
        And user state is "archived"
        And archive state is "<acts>_requested"
        And there are no unexpected requests to passport
        

        Given passport will respond without errors
        When we request with "action=<acts>"
        Then response status is 200
        And user state is "active"
        And archive state is "<acts>_in_progress"
        And there are no unexpected requests to passport
