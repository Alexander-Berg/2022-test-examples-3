Feature: Purge deleted messages

  Scenario: Delete and purge simple message
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    When we store "$1" into "inbox" with our new st_id
    And we delete "$1"
    Then "$1" is deleted
    And "inbox" is empty
    When we purge deleted message "$1"
    Then messages table is empty
    And our new st_id exist in storage delete queue

  Scenario: Delete and purge mulca-shared message
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    When we store "$1" into "inbox" with our new st_id and attributes "mulca-shared"
    And we delete "$1"
    Then "$1" is deleted
    And "inbox" is empty
    When we purge deleted message "$1"
    Then messages table is empty
    And storage delete queue is empty

  Scenario: Delete and purge message synced to subscriber should not add synced st_id to storage_delete_queue
    Given subscriber with "$1" synced message ready for purge
    When we delete "$1"
    And we purge deleted message "$1"
    Then storage delete queue is empty

  Scenario: Purge message with windat attachment message should add windat st_id to storage_delete_queue
    Given new initialized user
    And new st_id that does not exist in messages and storage delete queue
    When we store "$1" into "inbox"
    And we add windat attachment to "$1" with our new st_id
    And we delete "$1"
    When we purge deleted message "$1"
    Then our new st_id exist in storage delete queue
