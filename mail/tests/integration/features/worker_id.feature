Feature: Doberman should obtain free worker id

    Scenario: Doberman obtain free worker id on start
       Given doberman is stopped
         And free worker id "dobby"
        When I start doberman
        Then worker id "dobby" is assigned
