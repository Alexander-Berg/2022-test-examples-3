Feature: purge_deleted_box via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: purge_deleted_box request should work
    When we make purge_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Purge deleted messages after default ttl has expired(ttl=190 days)
    Given new user "Darunia" with messages in "inbox"
    And he deletes messages from "inbox" "200" days ago
    When we make purge_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "Darunia" deleted messages
    And there are some "Darunia" records in storage_delete_queue

  Scenario: Do not purge deleted messages before default ttl has expired
    Given new user "Gulley" with messages in "inbox"
    And he deletes messages from "inbox" right now
    When we make purge_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some "Gulley" deleted messages
    And there are no "Gulley" records in storage_delete_queue

  Scenario: Purge deleted messages after specified ttl has expired
    Given new user "Leroi" with messages in "inbox"
    And he deletes messages from "inbox" "5" days ago
    When we make purge_deleted_box request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "Leroi" deleted messages
    And there are some "Leroi" records in storage_delete_queue

  Scenario: Do not purge deleted messages before specified ttl has expired
    Given new user "Torton" with messages in "inbox"
    And he deletes messages from "inbox" "5" days ago
    When we make purge_deleted_box request with ttl_days "6"
    Then shiva responds ok
    And all shiva tasks finished
    And there are some "Torton" deleted messages
    And there are no "Torton" records in storage_delete_queue

  Scenario: Do not purge messages for transfered user
    Given new user "Makar" with messages in "inbox"
    And he deletes messages from "inbox" "200" days ago
    When we transfer "Makar" to different shard
    And we make purge_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some "Makar" deleted messages
    And there are no "Makar" records in storage_delete_queue
