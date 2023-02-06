Feature: purge_user via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: purge_transferred_user request should work
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: purge_deleted_user request should work
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call purge user after default purge_ttl(purge_ttl=1 day)
    Given new user "Fado" with messages in "inbox"
    And "Fado" transfered to different shard "2" days ago
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Fado" has not metadata in our shard
    And "Fado" is present in sharddb users

  Scenario: Call purge user before default purge_ttl
    Given new user "Rauru" with messages in "inbox"
    And "Rauru" transfered to different shard "0" days ago
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Rauru" has metadata in our shard
    And "Rauru" is present in sharddb users

  Scenario: Call purge_transferred_user after specified purge_ttl
    Given new user "Fabio" with messages in "inbox"
    And "Fabio" transfered to different shard "5" days ago
    When we make purge_transferred_user request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And "Fabio" has not metadata in our shard

  Scenario: Call purge_transferred_user before specified purge_ttl
    Given new user "Barry" with messages in "inbox"
    And "Barry" transfered to different shard "5" days ago
    When we make purge_transferred_user request with ttl_days "6"
    Then shiva responds ok
    And all shiva tasks finished
    And "Barry" has metadata in our shard

  Scenario: Call purge user in several processes
    Given 9 new users named as "Green" group
    And "Green" group of the 9 users transfered to different shard "2" days ago
    When we make purge_transferred_user request with jobs_count "2" and job_no "0"
    Then shiva responds ok
    And all shiva tasks finished
    When we make purge_transferred_user request with jobs_count "2" and job_no "1"
    Then shiva responds ok
    And all shiva tasks finished
    And "Green" group of the 9 users has not metadata in our shard

  Scenario: Purge user touch only not is_here users
    Given new user "Yoga" with messages in "inbox"
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Yoga" has metadata in our shard

  Scenario: Purge user with recods in storage_delete_queue
    Given new user "Fabia"
    And she has "$this" message in "inbox"
    When "$this" message was purged "3" days ago
    Given "Fabia" transfered to different shard "2" days ago
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Fabia" has not metadata in our shard

  Scenario: Purge deleted user
    Given new user "Navi" with messages in "inbox"
    And "Navi" was deleted "2" days ago
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Navi" has not metadata in our shard
    And "Navi" is absent in sharddb deleted users

  Scenario: Call purge_deleted_user after specified purge_ttl
    Given new user "Nelson" with messages in "inbox"
    And "Nelson" was deleted "5" days ago
    When we make purge_deleted_user request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And "Nelson" has not metadata in our shard

  Scenario: Call purge_deleted_user before specified purge_ttl
    Given new user "Neville" with messages in "inbox"
    And "Neville" was deleted "5" days ago
    When we make purge_deleted_user request with ttl_days "6"
    Then shiva responds ok
    And all shiva tasks finished
    And "Neville" has metadata in our shard

  Scenario: Call purge_transferred_user for deleted user
    Given new user "Rocky" with messages in "inbox"
    And "Rocky" was deleted "2" days ago
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Rocky" has metadata in our shard

  Scenario: Call purge_deleted_user for transferred user
    Given new user "Ricky" with messages in "inbox"
    And "Ricky" transfered to different shard "2" days ago
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Ricky" has metadata in our shard

  Scenario: Purge deleted user who is absent in sharddb
    Given new user "Valentine" with messages in "inbox"
    And "Valentine" was deleted "2" days ago
    But "Valentine" is absent in sharddb
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Valentine" has metadata in our shard

  Scenario: Purge deleted user who is not deleted in sharddb
    Given new user "Vere" with messages in "inbox"
    And "Vere" was deleted "2" days ago
    But "Vere" is not deleted in sharddb
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Vere" has metadata in our shard
    And "Vere" is present in sharddb users

  Scenario: Purge deleted user who is in different shard in sharddb
    Given new user "Vergil" with messages in "inbox"
    And "Vergil" was deleted "2" days ago
    But "Vergil" is in different shard in sharddb
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Vergil" has metadata in our shard
    And "Vergil" is present in sharddb deleted users

  Scenario: Purge transfered user who is absent in sharddb
    Given new user "Vernon" with messages in "inbox"
    And "Vernon" transfered to different shard "2" days ago
    But "Vernon" is absent in sharddb
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Vernon" has metadata in our shard

  Scenario: Purge transfered user who is in the original shard in sharddb
    Given new user "Vincent" with messages in "inbox"
    And "Vincent" transfered to different shard "2" days ago
    But "Vincent" is in the original shard in sharddb
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Vincent" has metadata in our shard

  Scenario: Purge transfered user who is absent in the destination shard in maildb
    Given new user "Vince" with messages in "inbox"
    And "Vince" transfered to different shard "2" days ago
    But "Vince" is absent in the destination shard in maildb
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Vince" has metadata in our shard

  Scenario: Purge transfered user with difference in destination maildb and sharddb
    Given new user "Virgil" with messages in "inbox"
    And "Virgil" transfered to different shard "2" days ago
    But "Virgil" is deleted in sharddb
    When we make purge_transferred_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Virgil" has metadata in our shard

  Scenario: Purge broken user with force flag
    Given new user "Vitold" with messages in "inbox"
    And "Vitold" was deleted "2" days ago
    But "Vitold" is absent in sharddb
    When we make purge_deleted_user request with force flag
    Then shiva responds ok
    And all shiva tasks finished
    And "Vitold" has not metadata in our shard

  Scenario: Purge deleted user with deleted_box
    Given new user "Alisher" with messages in "inbox"
    And "Alisher" was deleted "2" days ago
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "Alisher" has not metadata in our shard
    And there are some "Alisher" records in storage_delete_queue

  Scenario: Purge deleted user with backups
    Given new user "AlisherWithBackups" with messages in "inbox"
    And user has filled backup for "inbox"
    And he deletes messages from "inbox" "5" days ago
    And "AlisherWithBackups" was deleted "2" days ago
    When we make purge_deleted_box request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "AlisherWithBackups" deleted messages
    And there are no "AlisherWithBackups" records in storage_delete_queue
    When we make purge_deleted_user request
    Then shiva responds ok
    And all shiva tasks finished
    And "AlisherWithBackups" has not metadata in our shard
    And there are no backups for "AlisherWithBackups"
    And there are some "AlisherWithBackups" records in storage_delete_queue
