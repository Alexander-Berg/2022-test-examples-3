Feature: Operations with restores

  Scenario: Create restore without existing backup should produce backup_is_missing
    Given new initialized user
    When we create restore as "$create_op"
    Then "$create_op" produce "backup_is_missing"
    And user has no restores

  Scenario Outline: Create restore with existing backups not in complete should produce wrong_state
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "<b_state>" state
    When we create restore as "$create_op"
    Then "$create_op" produce "wrong_state"
    And user has no restores
    Examples:
      | b_state     |
      | in_progress |
      | inactive    |
      | error       |

  Scenario: Create restore with same created should produce unique_violation
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "complete" state
    When we create restore with same created as "$create_op"
    Then "$create_op" produce "unique_violation"
    And latest restore "state" is "complete"

  Scenario: Create restore with existing restore in_progress should produce unique_violation
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "in_progress" state
    When we create restore as "$create_op"
    Then "$create_op" produce "unique_violation"
    And latest restore "state" is "in_progress"

  Scenario: Cannot create restore for complete backup when another backup is running
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    When user has backup in "in_progress" state
    And we create restore as "$create_op" for previous backup
    Then "$create_op" produce "another_backup_in_running"

  Scenario: Create restore should create empty restore with in_progress state, method, mapping and to_restore_count
    Given new initialized user with "$[1:5]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has filled backup
    When we delete "$[1:3]"
    And we create "full_hierarchy" restore as "$create_op" with mapping
      | original | renewed |
      | 1        | 13      |
      | 42       | 666     |
    Then "$create_op" produce nothing
    And latest restore "state" is "in_progress"
    And latest restore "method" is "full_hierarchy"
    And latest restore "mapping" is
      | original | renewed |
      | 1        | 13      |
      | 42       | 666     |
    And latest restore "to_restore_count" is "3L"


  Scenario Outline: Fail restore not in in_progress should do nothing
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "<r_state>" state
    When we fail restore with notice "Some notice"
    Then latest restore "state" is "<r_state>"
    Then latest restore "notice" is null
    Examples:
      | r_state  |
      | complete |
      | error    |

  Scenario: Fail restore in in_progress should fail with notice
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "in_progress" state
    When we fail restore with notice "Some notice"
    Then latest restore "state" is "error"
    And latest restore "notice" is "Some notice"


  Scenario Outline: Complete restore not in in_progress should do nothing
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "<r-state>" state
    When we complete restore
    Then latest restore "state" is "<r-state>"
    Examples:
      | r-state  |
      | complete |
      | error    |

  Scenario: Complete restore in in_progress should complete
    Given new initialized user with "$[1:2]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has backup in "complete" state
    And user has restore in "in_progress" state
    When we complete restore
    Then latest restore "state" is "complete"


  Scenario: Restore update mids should update mids in backup, counter in restore and remove from deleted
    Given new initialized user with "$[1:5]" in "inbox"
    And folders with types "inbox" are in backup settings
    And user has filled backup
    When we delete "$[1:3]"
    And we create restore as "$create_op"
    And we update mids for "$[1:2]"
    Then latest restore "restored_count" is "2L"
    And backup_box has new mids for "$[1:2]"
    And backup_box does not have old mids for "$[1:2]"
    And deleted_box does not have new and old mids for "$[1:2]"
    And deleted_box has old mids for "$3"
