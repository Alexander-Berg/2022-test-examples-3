Feature: purge_backups via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: purge_backups request should work
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Purge backups after default ttl has expired(ttl=30 days)
    Given new user "BackupsAfterDefaultTtl" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "inactive" state
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupsAfterDefaultTtl"

  Scenario: Do not purge backups before default ttl has expired
    Given new user "BackupsBeforeDefaultTtl" with messages in "inbox"
    And user has filled backup for "inbox" "29" days ago in "inactive" state
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some backups for "BackupsBeforeDefaultTtl"

  Scenario: Purge backups after specified ttl has expired
    Given new user "BackupsAfterGivenTtl" with messages in "inbox"
    And user has filled backup for "inbox" "5" days ago in "inactive" state
    When we make purge_backups request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupsAfterGivenTtl"

  Scenario: Do not purge backups before specified ttl has expired
    Given new user "BackupsBeforeGivenTtl" with messages in "inbox"
    And user has filled backup for "inbox" "5" days ago in "inactive" state
    When we make purge_backups request with ttl_days "6"
    Then shiva responds ok
    And all shiva tasks finished
    And there are some backups for "BackupsBeforeGivenTtl"

  Scenario: Purge backups in error state
    Given new user "ErrorBackup" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "error" state
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "ErrorBackup"

  Scenario Outline: Do not purge backups in active states
    Given new user "<user>" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "<backup_state>" state
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some backups for "<user>"

    Examples:
    | user    | backup_state  |
    | InProgressBackup  | in_progress   | 
    | CompleteBackup    | complete      | 

  Scenario: Purge backups cleans restores
    Given new user "BackupWithRestore" with messages in "inbox"
    And user has filled backup for "inbox"
    And user has restore
    And backup change state to "inactive" "31" days ago
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupWithRestore"
    And there are no restores for "BackupWithRestore"

  Scenario: Purge backups for user with live messages adds nothing to storage_delete_queue 
    Given new user "BackupWithLiveMessages" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "inactive" state
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupWithLiveMessages"
    And there are no "BackupWithLiveMessages" records in storage_delete_queue

  Scenario: Purge backups for user with deleted messages adds nothing to storage_delete_queue 
    Given new user "BackupWithDeletedMessages" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "inactive" state
    And he deletes messages from "inbox" right now
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupWithDeletedMessages"
    And there are no "BackupWithDeletedMessages" records in storage_delete_queue

  Scenario: Purge backups for user with purged messages adds stids to storage_delete_queue 
    Given new user "BackupWithPurgedMessages" with messages in "inbox"
    And user has filled backup for "inbox" "31" days ago in "inactive" state
    And he deletes messages from "inbox" "5" days ago
    When we make purge_deleted_box request with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "BackupWithPurgedMessages" deleted messages
    And there are no "BackupWithPurgedMessages" records in storage_delete_queue
    When we make purge_backups request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no backups for "BackupWithPurgedMessages"
    And there are some "BackupWithPurgedMessages" records in storage_delete_queue
