Feature: pnl_estimation_export via shiva

  Background: Should not have shiva tasks or yt yables
    Given there are no shiva tasks
    And there are no active users in shard
    And there are no tables in yt for pnl estimation

  Scenario: Should create table with known fields in schema
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl deleted estimation with schema
      | name               | type   |
      | from_shard_id      | uint64 |
      | message_count      | uint64 |
      | approx_bytes_in_db | uint64 |
    And there are empty shard table in yt for pnl mailbox estimation with schema
      | name               | type      |
      | uid                | uint64    |
      | from_shard_id      | uint64    |
      | here_since         | timestamp |
      | folders_count      | uint64    |
      | message_count      | uint64    |
      | message_size       | uint64    |
      | attach_count       | uint64    |
      | attach_size        | uint64    |
      | approx_bytes_in_db | uint64    |
      | move               | uint64    |
      | delete             | uint64    |
      | movel              | uint64    |
      | status             | uint64    |
      | forward            | uint64    |
      | forwardwithstore   | uint64    |
      | reply              | uint64    |
      | notify             | uint64    |

  Scenario: Should dump deleted info
    Given new user "Wina" with messages in "inbox"
    And he deletes messages from "inbox" right now
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl deleted estimation with 1 rows
      | from_shard_id | message_count | approx_bytes_in_db |
      | $shard_id     | !null         | !null              |

  Scenario: Should dump mailbox info
    Given new user "Yumi" with "5" messages in "inbox"
    And she has "3" messages in "trash"
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl mailbox estimation with 1 rows
      | uid     | folders_count | message_count | message_size | approx_bytes_in_db |
      | Yumi    | 6             | 8             | !null        | !null              |

  Scenario: Should dump filters info
    Given new user "Azura"
    And "Azura" has enabled filter "forward"
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl mailbox estimation with 1 rows
      | uid     | move | delete | movel | status | forward | forwardwithstore | reply | notify |
      | Azura   | null | null   | null  | null   | 1       | null             | null  | null   |

  Scenario: Should dump alive users
    Given new user "Shiri"
    And he is in shard since "2020-02-02 20:20:20.12345"
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl mailbox estimation with 1 rows
      | uid     | from_shard_id | here_since    |
      | Shiri   | $shard_id     | 1580664020123 |

  Scenario: Should not dump deleted users
    Given new user "Berta"
    And "Berta" was deleted "7" days ago
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are empty shard table in yt for pnl mailbox estimation

  Scenario: Should not dump transferred users
    Given new user "Miron"
    And "Miron" transfered to different shard "2" days ago
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are empty shard table in yt for pnl mailbox estimation

  Scenario Outline: Should not dump users in deleted or archived states
    Given new user "<user>"
    And he is in "<state>" state "0" days
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are empty shard table in yt for pnl mailbox estimation

    Examples:
    | user    | state    |
    | Atticus | deleted  |
    | Herman  | archived |

  Scenario Outline: Should dump users in other states
    Given new user "<user>"
    And he is in "<state>" state "0" days
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl mailbox estimation with row for "<user>"

    Examples:
    | user    | state    |
    | Hodei   | active   |
    | Ties    | inactive |
    | Cormag  | notified |
    | Nadim   | frozen   |
    | Roeland | special  |

  Scenario: Should ignore dumped users
    Given new user "Gilles" with "5" messages in "inbox"
    And there are shard table in yt for pnl mailbox estimation with row for "Gilles"
    And he deletes messages from "inbox" right now
    When we make pnl_estimation_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for pnl mailbox estimation with 1 rows
      | uid     | message_count |
      | Gilles  | 5             |
