Feature: purge_synced_deleted_box via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: purge_synced_deleted_box request should work
    When we make purge_synced_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Purge synced deleted messages
    Given new user "Alaric" with messages in "inbox"
    And his messages have synced attribute
    And he deletes messages from "inbox" right now
    When we make purge_synced_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "Alaric" deleted messages
    And there are no "Alaric" records in storage_delete_queue

  Scenario: Do not purge general deleted messages
    Given new user "Aldrich" with messages in "inbox"
    And he deletes messages from "inbox" "1000" days ago
    When we make purge_synced_deleted_box request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some "Aldrich" deleted messages
    And there are no "Aldrich" records in storage_delete_queue
