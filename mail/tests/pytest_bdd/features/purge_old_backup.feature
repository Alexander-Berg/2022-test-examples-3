Feature: Purge old backups

  Scenario: Purge specified user backups
    Given new initialized user with "$1" in "inbox"
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    And user has restore in "complete" state
    When we purge current backup
    Then in this tables his data exists
      | table_name               |
      | backup.folders_to_backup |
      | backup.tabs_to_backup    |
    Then in this tables there are no his data
      | table_name               |
      | backup.backups           |
      | backup.restores          |
      | backup.folders           |
      | backup.box               |

  Scenario: Purge specified user backups does not delete the others
    Given new initialized user with "$1" in "inbox"
    And folders with types "inbox" and tabs "relevant" are in backup settings
    When user has filled backup
    And user has restore in "complete" state
    And user has filled backup
    And user has restore in "complete" state
    When we purge previous backup
    Then previous backup does not exist
    And current backup is filled
    And restore for current backup exists

  Scenario: Purge specified user backups with live messages adds nothing to storage_delete_queue 
    Given new initialized user with "$1" in "inbox" with new st_id
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    When we purge current backup
    Then storage delete queue is empty

  Scenario: Purge specified user backups with deleted messages adds nothing to storage_delete_queue 
    Given new initialized user with "$1" in "inbox" with new st_id
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    When we delete "$1"
    Then storage delete queue is empty
    When we purge current backup
    Then storage delete queue is empty

  Scenario: Purge specified user backups with purged general messages adds stids to storage_delete_queue 
    Given new initialized user with "$1" in "inbox" with new st_id
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    When we delete "$1"
    And we purge deleted message "$1"
    Then storage delete queue is empty
    When we purge current backup
    Then our new st_id exist in storage delete queue

  Scenario: Purge specified user backups with purged mulca-shared messages adds nothing to storage_delete_queue 
    Given new initialized user with "$1" in "inbox" with new st_id and attributes "mulca-shared"
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    When we delete "$1"
    And we purge deleted message "$1"
    When we purge current backup
    Then storage delete queue is empty
