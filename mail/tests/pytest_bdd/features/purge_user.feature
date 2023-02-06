Feature: Purge user

  Scenario: Purge new user
    Given new initialized user
    When we purge him
    Then in this tables there are no his data
      | table_name    |
      | mail.users    |
      | mail.counters |
      | mail.folders  |
      | mail.labels   |


  Scenario: Purge user with filters
    Given new initialized user
    When he create filter "Move apples to trash"
        """
        IF from contains apple THEN move to trash
        """
    And we purge user
    Then in this tables there are no his data
      | table_name         |
      | filters.rules      |
      | filters.conditions |


  Scenario: Purge user with black list
    Given new initialized user
    When we add "spam@spammer" to blacklist
    And we purge user
    Then in table "filters.elist" there are no his data


  Scenario: Purge user with message
    Given new initialized user with "$1" in "inbox"
    When we purge him
    Then in this tables there are no his data
      | table_name              |
      | mail.box                |
      | mail.messages           |
      | mail.message_references |
      | mail.threads_messages   |
      | mail.threads            |


  Scenario: Purge user with deleted message
    Given new initialized user with "$1" in "inbox"
    When we delete "$1"
    And we purge user
    Then in table "mail.deleted_box" there are no his data


  Scenario: Purge user with POP3 meta
    Given new user with popped "inbox"
    When we store "$1" into "inbox"
    And we purge him
    Then in table "mail.pop3_box" there are no his data


  Scenario: Purge user with records in storage_delete_queue
    Given new initialized user with "$1" in "inbox"
    And he has in storage delete queue
      | st_id     | deleted_date            |
      | 4.8.15.16 | 2017-10-04 00:00:00 UTC |
    When we delete user
    And we purge user
    Then in table "mail.storage_delete_queue" his data exists


  Scenario: Purge user with records in change_log
    Given new initialized user with "$1" in "inbox"
    When we delete user
    And we purge user
    Then in table "mail.change_log" his data exists


  @MAILPG-528 @purge-steps
  Scenario: Purge user with more than `impl.purge_mids_chunk_size` messages
    Given new initialized user
    When we store "$[1:5]" into "inbox"
    And we purge user
    Then in this tables there are no his data
      | table_name    |
      | mail.users    |
      | mail.messages |


  Scenario: Purge user with windat attachments
    Given new initialized user with "$1" in "inbox"
    When we add windat attachment to "$1"
    And we purge user
    Then in table "mail.windat_messages" there are no his data


  Scenario: Purge user in one transcation at least we need this procedure in transfer
    Given new initialized user with "$[1:5]" in "inbox"
    When we purge him in one transaction
    Then in table "mail.users" there are no his data

  @other-user
  Scenario: Purge user do not touch other users
    Given new initialized user with "$[1:5]" in "inbox"
    And replication stream
    When we purge him in one transaction
    Then there are only our user changes in replication stream


  Scenario: Purge user on delete
    Given new initialized user
    When we purge him on delete
    Then in table "mail.users" his data exists
    And in this tables there are no his data
      | table_name   |
      | mail.folders |
      | mail.labels  |

  Scenario: Purge user with message on delete
    Given new initialized user with "$1" in "inbox"
    When we purge him on delete
    Then in this tables his data exists
      | table_name    |
      | mail.users    |
      | mail.messages |
    And in this tables there are no his data
      | table_name              |
      | mail.box                |
      | mail.message_references |
      | mail.threads_messages   |
      | mail.threads            |

  Scenario: Purge user with deleted message on delete
    Given new initialized user with "$1" in "inbox"
    When we delete "$1"
    And we purge user on delete
    Then in this tables his data exists
      | table_name       |
      | mail.users       |
      | mail.messages    |
      | mail.deleted_box |

  Scenario: Purge user with backups
    Given new initialized user with "$1" in "inbox"
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    And user has restore in "complete" state
    When we purge him
    Then in this tables there are no his data
      | table_name               |
      | backup.backups           |
      | backup.restores          |
      | backup.folders_to_backup |
      | backup.tabs_to_backup    |
      | backup.folders           |
      | backup.box               |

  Scenario: Purge user with backups on delete
    Given new initialized user with "$1" in "inbox"
    And folders with types "inbox" and tabs "relevant" are in backup settings
    And user has filled backup
    And user has restore in "complete" state
    When we purge user on delete
    Then in this tables his data exists
      | table_name               |
      | backup.backups           |
      | backup.restores          |
      | backup.folders           |
      | backup.box               |
    Then in this tables there are no his data
      | table_name               |
      | backup.folders_to_backup |
      | backup.tabs_to_backup    |
