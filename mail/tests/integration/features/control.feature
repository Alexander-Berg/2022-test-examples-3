Feature: Doberman control interface

    Scenario: Restart doberman
       Given doberman is running
        When I stop doberman
        Then doberman process is stopped
        When I start doberman
        Then doberman process is running


    Scenario: Rotate log
       Given doberman is running
        When I send "reopen_log" command to doberman
        Then doberman response is "Ok"
         And doberman process is running
