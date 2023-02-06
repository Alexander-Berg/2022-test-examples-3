Feature: Affinities
    Background:
        Given segments 217/13,216/14

    Scenario: Compute affinities
        When stats G has 1000 out of 10000 for each segment
        When stats L has 200 out of 1000 for each segment
        When we compute affinities of L over G
        Then all affinities are approximately 1.5

    Scenario: Compute affinities with empty stats
        When stats G has 1000 out of 10000 for each segment
        When stats L is empty
        When we compute affinities of L over G
        Then all affinities are approximately 1.0
