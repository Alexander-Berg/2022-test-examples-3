Feature: close_for_load via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: close_for_load request should work
    When we make close_for_load request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: close_for_load request should update used_size
    Given current shard is shiva shard with disk size "1000"
    When we make close_for_load request
    Then shiva responds ok
    And all shiva tasks finished
    And current shard has updated used_size

  Scenario: Call close_for_load for overloaded shard
    Given new user "Sally"
    And current shard is shiva shard with disk size "1000"
    When we make close_for_load request
    Then shiva responds ok
    And all shiva tasks finished
    And transfer for current shard closed
    And registration for current shard closed

  Scenario: Call close_for_load for non-overloaded shard
    Given new user "Sammy"
    And current shard is shiva shard with disk size "10000000000"
    When we make close_for_load request
    Then shiva responds ok
    And all shiva tasks finished
    And transfer for current shard opened
    And registration for current shard opened
