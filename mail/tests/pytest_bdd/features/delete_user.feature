Feature: Delete user

  Scenario: Delete user with no messages
    Given new initialized user
    When we delete user
    Then user is here
    And user is deleted
    And storage delete queue is empty
    And in changelog there is
      | revision | type        |
      | 2        | user-delete |


  Scenario: Delete user with message and windat attachment
    Given new initialized user
    When we store into "inbox"
      | mid | st_id     |
      | $1  | 4.8.15.16 |
    And we add windat attachment to "$1" with "5.windat.E6:7" st_id
    And we delete user at "2016-10-04 00:00:00 UTC"
    Then in changelog there is
      | revision | type        |
      | 3        | user-delete |

  Scenario: Delete user with mulca-shared message
    Given new initialized user
    When we store into "inbox"
      | mid | st_id   | attributes   |
      | 1   | 1.2.3.4 | mulca-shared |
    And we delete user
    Then storage delete queue is empty
    And in changelog there is
      | revision | type        |
      | 3        | user-delete |

  Scenario: Delete user with synced message should not add synced st_ids to storage_delete_queue
    Given subscriber with synced message ready for purge
    When we delete user
    Then storage delete queue is empty

  @other-user
  Scenario: Delete user touch only our user
    Given new initialized user with "$1" in "inbox"
    And replication stream
    When we delete user
    Then there are only our user changes in replication stream

  Scenario: Delete user updates newer deleted_date in storage_delete_queue
    Given new initialized user
    And he has in storage delete queue
      | st_id     | deleted_date            |
      | 4.8.15.16 | 2017-10-04 00:00:00 UTC |
    When we delete user at "2016-10-04 00:00:00 UTC"
    Then in storage delete queue there is
      | st_id     | deleted_date            |
      | 4.8.15.16 | 2016-10-04 00:00:00 UTC |

  Scenario: Delete user does not update older deleted_date in storage_delete_queue
    Given new initialized user
    And he has in storage delete queue
      | st_id     | deleted_date            |
      | 4.8.15.16 | 2015-10-04 00:00:00 UTC |
    When we delete user at "2016-10-04 00:00:00 UTC"
    Then in storage delete queue there is
      | st_id     | deleted_date            |
      | 4.8.15.16 | 2015-10-04 00:00:00 UTC |

  Scenario: Delete user move messages to deleted box
    Given new initialized user
    When we store into "inbox"
      | mid | st_id     |
      | $1  | 4.8.15.16 |
    And we delete user at "2016-10-04 00:00:00 UTC"
    Then in table "box" by "mid" key there are no rows at "$1"
    Then in table "deleted_box" by "mid" key there is
      | mid |
      | $1  |

  Scenario: Delete user resets user metadata
    Given new initialized user
    When we create "user" folder "cherry"
    And we delete user at "2016-10-04 00:00:00 UTC"
    Then user has folders initialized at revision "3"
      | type   |
      | inbox  |
      | spam   |
      | trash  |
      | sent   |
      | outbox |
      | drafts |

  Scenario: Delete user should reset attach counters
    Given new initialized user
    When we store into "inbox"
      | mid | attaches                  |
      | $1  | 1.2:image/jpg:apple.jpg:0 |
    And we delete user at "2016-10-04 00:00:00 UTC"
    Then user has no messages with attaches
