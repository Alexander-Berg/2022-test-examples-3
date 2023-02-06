Feature: end prepared transaction via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: end_prepared_transaction request should work
    When we make end_prepared_transaction request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Rollback prepared transaction in maildb for absent user in sharddb
    Given new user "Dilan" in blackbox
    And "Dilan" does not exists in sharddb
    And sharpei creates prepared transaction for "Dilan" in maildb 
    When we make end_prepared_transaction request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no prepared transaction in maildb 
    And "Dilan" has not metadata in our shard

  Scenario: Commit prepared transaction in maildb for existing user in sharddb
    Given new user "Drake" in blackbox
    And "Drake" exists in sharddb
    And sharpei creates prepared transaction for "Drake" in maildb 
    When we make end_prepared_transaction request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no prepared transaction in maildb
    And "Drake" has metadata in our shard
