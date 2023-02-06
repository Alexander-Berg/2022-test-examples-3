Feature: Folder archivation rules management

    # Set archivation rules
  Scenario Outline: Set rule for folder
    Given new initialized user
    When we set "<type>" rule for "inbox" with 30 days ttl and <rule_max_size> max size
    Then "inbox" has archivation rule
      | archive_type | keep_days | max_size       |
      | <type>       | 30        | <max_size_exp> |
    Examples:
      | type    | rule_max_size | max_size_exp |
      | archive | null          | 1000000      |
      | clean   | null          | 1000000      |
      | archive | 200           | 200          |
      | clean   | 200           | 200          |

  @other-user
  Scenario: Set rule for folder affects only our user
    Given new initialized user
    And replication stream
    When we set "archive" rule for "inbox"
    Then there are only our user changes in replication stream

  Scenario: Set rule for folder writes to changelog
    Given new initialized user
    When we set "archive" rule for "inbox"
    Then "set-archivation-rule" is last changelog entry

  Scenario: Set rule for folder does not write to shared folder change queue
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we set "archive" rule for "inbox"
    Then "Efim" change queue for "inbox@bbs" is empty

  Scenario: Set rule for folder increments revision
    Given new initialized user
    Then global revision is "1"
    When we set "archive" rule for "inbox"
    Then global revision is "2"

  Scenario: Set rule for folder updates previous rule
    Given new initialized user
    When we set "archive" rule for "inbox" with 30 days ttl
    Then "inbox" has archivation rule
      | archive_type | keep_days | revision | max_size |
      | archive      | 30        | 2        | 1000000  |
    When we set "clean" rule for "inbox" with 180 days ttl and 1000 max size
    Then "inbox" has archivation rule
      | archive_type | keep_days | revision | max_size |
      | clean        | 180       | 3        | 1000     |

    # Remove archivation rules
  Scenario: Remove rule for folder removes rule
    Given new initialized user with "archive" rule for "inbox"
    When we remove archivation rule for "inbox"
    Then "inbox" has no archivation rules

  @other-user
  Scenario: Remove rule for folder affects only our user
    Given new initialized user with "archive" rule for "inbox"
    And replication stream
    When we remove archivation rule for "inbox"
    Then there are only our user changes in replication stream

  Scenario: Remove rule for folder writes to changelog
    Given new initialized user with "archive" rule for "inbox"
    When we remove archivation rule for "inbox"
    Then "remove-archivation-rule" is last changelog entry

  Scenario: Remove rule for folder does not write to shared folder change queue
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we set "archive" rule for "inbox" with 30 days ttl
    Then "inbox" has archivation rule
    When we remove archivation rule for "inbox"
    Then "Efim" change queue for "inbox@bbs" is empty

  Scenario: Remove rule for folder increments revision
    Given new initialized user with "archive" rule for "inbox"
    Then global revision is "2"
    When we remove archivation rule for "inbox"
    Then global revision is "3"

  Scenario: Remove unexisting rule for folder does nothing
    Given new initialized user
    Then global revision is "1"
    When we remove archivation rule for "inbox"
    Then global revision is "1"
