Feature: Add to storage delete queue

  Scenario: Add to storage delete queue single message
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    When we add our new st_id to storage delete queue
    Then our new st_id exist in storage delete queue

  Scenario: Add to storage delete queue message with copy
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    When we store "$1" into "inbox" with our new st_id
    And we copy "$1" to "inbox"
    And we delete "$1"
    Then "$1" is deleted
    And storage delete queue is empty

  Scenario: Add to storage delete queue message with backup
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    And folders with types "inbox" are in backup settings
    When we store "$1" into "inbox" with our new st_id
    And we create backup with limit "10" and with tabs disabled as "$create_op"
    And we fill backup with tabs disabled as "$fill_op"
    And we delete "$1"
    Then "$1" is deleted
    And backup_box has old mids for "$1"
    And storage delete queue is empty
