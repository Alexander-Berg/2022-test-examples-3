Feature: Checking black and white lists functionality 

    Background:
        Given furita is up and running

    Scenario Outline: Creating and obtaining entity from the black or white lists
        Given new <user>

        When we add "my@test.mail" to the <list_type> of the <user>
        Then furita replies with 200

        When we obtain the <list_type> of the <user>
        Then furita replies with 200
        And ordered obtained list is the following:
            "my@test.mail"

        Examples: Vertical
        | user      | FiliTheWhite | FiliTheBlack |
        | list_type | whitelist    | blacklist    |

    Scenario Outline: Creating and removing the entity from the black or white lists
        Given new <user>

        When we add "my@test.mail" to the <list_type> of the <user>
        Then furita replies with 200

        When we obtain the <list_type> of the <user>
        Then furita replies with 200
        And ordered obtained list is the following:
            "my@test.mail"

        When we remove the following records from the <list_type> of the <user>:
            "my@test.mail"
        Then furita replies with 200

        When we obtain the <list_type> of the <user>
        Then furita replies with 200
        And there are no records in the obtained list

        Examples: Vertical
        | user      | OinTheWhite | OinTheBlack |
        | list_type | whitelist   | blacklist   |

    Scenario Outline: Creating the same entity twice in the black or white lists
        Given new <user>

        When we add "my@test.mail" to the <list_type> of the <user>
        Then furita replies with 200

        When we add "my@test.mail" to the <list_type> of the <user>
        Then furita replies with 500 and error message is "Already exists"

        Examples: Vertical
        | user      | KiliTheWhite | KiliTheBlack |
        | list_type | whitelist    | blacklist    |

    Scenario Outline: Removing unexistent entity from the black or white lists
        Given new <user>

        When we add "my@test.mail" to the <list_type> of the <user>
        Then furita replies with 200

        When we remove the following records from the <list_type> of the <user>:
            "just.another@test.mail"
        Then furita replies with 200

        When we obtain the <list_type> of the <user>
        Then furita replies with 200
        And ordered obtained list is the following:
            "my@test.mail"

        Examples: Vertical
        | user      | GloinTheWhite | GloinTheBlack |
        | list_type | whitelist     | blacklist     |

    Scenario Outline: Creating entity in the one list type while the same entity is already in another list type
        Given new <user>

        When we add "my@favorite.mail" to the <one_list_type> of the <user>
        Then furita replies with 200

        When we add "my@favorite.mail" to the <another_list_type> of the <user>
        Then furita replies with 500 and error message is "Already exists"

        Examples: Vertical
        | user              | BifurTheWhite | BifurTheBlack |
        | one_list_type     | whitelist     | blacklist     |
        | another_list_type | blacklist     | whitelist     |

