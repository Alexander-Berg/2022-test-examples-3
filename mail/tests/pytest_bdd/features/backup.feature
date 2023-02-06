Feature: Operations with backups

  Scenario: Update backup setting for new user should set settings
    Given new initialized user
    When we set to backup folders with types "inbox,sent" and tabs "relevant,news"
    Then folders with types "inbox,sent" and tabs "relevant,news" are in backup settings

  Scenario: Update backup setting for user with settings should update them
    Given new initialized user
    And folders with types "inbox,sent" are in backup settings
    When we set to backup folders with types "trash,sent"
    Then folders with types "sent,trash" are in backup settings

  Scenario: Update backup setting should return current settings
    Given new initialized user
    When we set to backup folders with types "inbox,sent"
    Then update returns current backup settings

  Scenario: Update backup setting should add inbox if relevant is set
    Given new initialized user
    When we set to backup folders with types "sent" and tabs "relevant"
    Then folders with types "inbox,sent" and tabs "relevant" are in backup settings

  Scenario: Update backup setting should add relevant if inbox is set
    Given new initialized user
    When we set to backup folders with types "inbox" and tabs "news"
    Then folders with types "inbox" and tabs "relevant,news" are in backup settings


  Scenario: Reserve backup_id should increase serials
    Given new initialized user
    Then next_backup_id is "1"
    When we reserve backup_id
    Then reserved backup_id is "1"
    And next_backup_id is "2"

  @concurrent
  Scenario: Concurrent reserve backup_id
    Given new initialized user
    Then next_backup_id is "1"
    When we try reserve backup_id as "$first"
    And we try reserve backup_id as "$second"
    And we commit "$first"
    And we commit "$second"
    Then reserved backup_ids in "$first" and "$second" are different
    And next_backup_id is "3"


  Scenario: Create backup without reserving backup_id should produce backup_id_not_reserved
    Given new initialized user
    When we create backup without reserving backup_id as "$create_op"
    Then "$create_op" produce "backup_id_not_reserved"
    And user has no backups

  Scenario: Create backup without settings should produce empty_folder_list
    Given new initialized user
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce "empty_folder_list"
    And user has no backups

  Scenario: Create backup for empty folders should produce nothing_to_backup
    Given new initialized user
    And folders with types "inbox" are in backup settings
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce "nothing_to_backup"
    And user has no backups

  Scenario: Create backup for empty tabs should produce nothing_to_backup
    Given new initialized user
    And tabs "news" are in backup settings
    When we create backup with limit "10" and with tabs enabled as "$create_op"
    Then "$create_op" produce "nothing_to_backup"
    And user has no backups

  Scenario: Create backup with relevant tab enabled should not count all inbox messages
    Given new initialized user
    And folders with types "inbox" and tabs "relevant" are in backup settings
    When we store "$1, $2" into tab "relevant"
    And we store "$3, $4" into "inbox"
    When we create backup with limit "3" and with tabs enabled as "$create_op"
    Then "$create_op" produce nothing
    And current backup "state" is "in_progress"

  Scenario: Create backup for exceeding messages limit should produce too_many_messages
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    When we create backup with limit "1" and with tabs disabled as "$create_op"
    Then "$create_op" produce "too_many_messages"
    And user has no backups

  Scenario: Create backup with folder should create empty backup with in_progress state
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce nothing
    And current backup "state" is "in_progress"
    And current backup is empty
    And current backup is the only backup

  Scenario: Create backup with tab should create empty backup with in_progress state
    Given new initialized user with "$[1:2]" in tab "news"
    And tabs "news" are in backup settings
    When we create backup with limit "10" and with tabs enabled as "$create_op"
    Then "$create_op" produce nothing
    And current backup "state" is "in_progress"
    And current backup is empty
    And current backup is the only backup

  Scenario: Create backup with folder and tab should create empty backup with in_progress state
    Given new initialized user with "$[1:2]" in tab "news"
    And folders with types "inbox", tabs "news" are in backup settings
    When we store "$3" into "inbox"
    And we create backup with limit "10" and with tabs enabled as "$create_op"
    Then "$create_op" produce nothing
    And current backup "state" is "in_progress"
    And current backup is empty
    And current backup is the only backup

  Scenario: Create backup with same id should produce unique_violation
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    When we create backup with same id with limit "10" as "$create_op"
    Then "$create_op" produce "unique_violation"
    And current backup is the only backup

  Scenario: Create backup with existing backup in_progress should produce unique_violation
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "in_progress" state
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce "unique_violation"
    And current backup does not exist
    And previous backup is the only backup

  Scenario: Create backup with running restore should produce running_restore
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "in_progress" state
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce "running_restore"
    And current backup does not exist
    And previous backup is the only backup

  Scenario Outline: Create backup with existing backups should create empty backup with in_progress state
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "<b_state>" state
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    Then "$create_op" produce nothing
    And current backup "state" is "in_progress"
    And current backup is empty
    And previous backup "state" is "<b_state>"
    Examples:
      | b_state  |
      | complete |
      | inactive |
      | error    |


  Scenario Outline: Fail backup not in in_progress should do nothing
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "<b_state>" state
    When we fail backup with notice "Some notice"
    Then current backup "state" is "<b_state>"
    And current backup "notice" is null
    Examples:
      | b_state  |
      | complete |
      | inactive |
      | error    |

  Scenario: Fail backup in in_progress should fail with notice
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "in_progress" state
    When we fail backup with notice "Some notice"
    Then current backup "state" is "error"
    And current backup "notice" is "Some notice"


  Scenario: Deactivate backup without existing backups should produce missing_complete_backup
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    When we deactivate backup as "$delete_op"
    Then "$delete_op" produce "missing_complete_backup"
    And user has no backups

  Scenario Outline: Deactivate backup with existing backups not in complete should produce missing_complete_backup
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "<b_state>" state
    When we deactivate backup as "$delete_op"
    Then "$delete_op" produce "missing_complete_backup"
    And current backup "state" is "<b_state>"
    Examples:
      | b_state     |
      | in_progress |
      | inactive    |
      | error       |

  Scenario: Deactivate backup with running restore should produce restore_in_progress
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "in_progress" state
    When we deactivate backup as "$delete_op"
    Then "$delete_op" produce "restore_in_progress"
    And current backup "state" is "complete"

  Scenario: Deactivate backup with existing backups in complete should set inactive
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    When we deactivate backup as "$delete_op"
    Then "$delete_op" produce nothing
    And current backup "state" is "inactive"


  Scenario: Fill backup without existing backup should produce backup_is_missing
    Given new initialized user
    When we fill backup with tabs disabled as "$fill_op"
    Then "$fill_op" produce "backup_is_missing"

  Scenario Outline: Fill backup with existing backup not in in_progress should produce wrong_state
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "<b_state>" state
    When we fill backup with tabs disabled as "$fill_op"
    Then "$fill_op" produce "wrong_state"
    And current backup "state" is "<b_state>"
    Examples:
      | b_state  |
      | complete |
      | inactive |
      | error    |

  Scenario: Fill backup with empty folders should produce nothing_to_backup and set error state
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "in_progress" state
    When we delete "$[1:2]"
    And we fill backup with tabs disabled as "$fill_op"
    Then "$fill_op" produce "nothing_to_backup"
    And current backup "state" is "error"

  Scenario: Fill backup should complete current and deactivate previous
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    When we create backup with limit "10" and with tabs disabled as "$create_op"
    And we fill backup with tabs disabled as "$fill_op"
    Then "$fill_op" produce nothing
    And current backup "state" is "complete"
    And previous backup "state" is "inactive"

  Scenario: Fill backup with tabs disabled should fill folders and box from full inbox
    Given new initialized user
    When we create "user" folder "basket"
    And we create "user" folder "apple" under "basket"
    And we store into tab "relevant"
      | mid | st_id | received_date           |
      | $1  | 111   | 2020-03-08 01:00:00 UTC |
    And we store into tab "news"
      | mid | st_id | received_date           |
      | $2  | 222   | 2020-03-08 02:00:00 UTC |
    And we store into tab "social"
      | mid | st_id | received_date           |
      | $3  | 333   | 2020-03-08 03:00:00 UTC |
    And we store into folder named "basket|apple"
      | mid | st_id | received_date           |
      | $4  | 444   | 2020-03-08 04:00:00 UTC |
    And we set to backup tabs "relevant,news" and folders with names "apple"
    And we create backup with limit "10" and with tabs disabled as "$create_op"
    And we fill backup with tabs disabled as "$fill_op"
    Then "$fill_op" produce nothing
    And current backup "message_count" is "4L"
    And in backup_folders there are
      | fid                | type  | name   | parent_fid   | is_backuped |
      | #type:inbox        | inbox | Inbox  |              | True        |
      | #name:basket       | user  | basket |              | False       |
      | #name:basket/apple | user  | apple  | #name:basket | True        |
    And in backup_box there are messages
      | mid | st_id | fid                | tab      | received_date           |
      | $1  | 111   | #type:inbox        | relevant | 2020-03-08 01:00:00 UTC |
      | $2  | 222   | #type:inbox        | news     | 2020-03-08 02:00:00 UTC |
      | $3  | 333   | #type:inbox        | social   | 2020-03-08 03:00:00 UTC |
      | $4  | 444   | #name:basket/apple |          | 2020-03-08 04:00:00 UTC |

Scenario: Fill backup with tabs disabled should fill folders and box from tabs
  Given new initialized user
  When we create "user" folder "basket"
  And we create "user" folder "apple" under "basket"
  And we store into tab "relevant"
    | mid | st_id | received_date           |
    | $1  | 111   | 2020-03-08 01:00:00 UTC |
  And we store into tab "news"
    | mid | st_id | received_date           |
    | $2  | 222   | 2020-03-08 02:00:00 UTC |
  And we store into tab "social"
    | mid | st_id | received_date           |
    | $3  | 333   | 2020-03-08 03:00:00 UTC |
  And we store into folder named "basket|apple"
    | mid | st_id | received_date           |
    | $4  | 444   | 2020-03-08 04:00:00 UTC |
  And we set to backup tabs "relevant,news" and folders with names "apple"
  And we create backup with limit "10" and with tabs enabled as "$create_op"
  And we fill backup with tabs enabled as "$fill_op"
  Then "$fill_op" produce nothing
  And current backup "message_count" is "3L"
  And in backup_folders there are
    | fid                | type  | name   | parent_fid   | is_backuped |
    | #type:inbox        | inbox | Inbox  |              | True        |
    | #name:basket       | user  | basket |              | False       |
    | #name:basket/apple | user  | apple  | #name:basket | True        |
  And in backup_box there are messages
    | mid | st_id | fid                | tab      | received_date           |
    | $1  | 111   | #type:inbox        | relevant | 2020-03-08 01:00:00 UTC |
    | $2  | 222   | #type:inbox        | news     | 2020-03-08 02:00:00 UTC |
    | $4  | 444   | #name:basket/apple |          | 2020-03-08 04:00:00 UTC |
