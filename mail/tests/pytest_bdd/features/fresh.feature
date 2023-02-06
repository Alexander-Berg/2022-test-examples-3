Feature: Reset fresh counter

  Scenario: Reset non-0 fresh
    Given new initialized user
    When we store "$1" into "inbox"
    Then fresh counter is "1" and has revision "2"
    And global revision is "2"
    When we reset fresh as "reset_fresh"
    Then "reset_fresh" result is "3"
    Then fresh counter is "0" and has revision "3"
    And global revision is "3"

  Scenario: Reset 0 fresh
    Given new initialized user
    When we reset fresh as "reset_fresh"
    Then "reset_fresh" result is "0"
    Then fresh counter is "0" and has revision "1"
    And global revision is "1"

  Scenario: Write to changelog
    Given new initialized user
    When we store "$1" into "inbox"
    Then fresh counter is "1" and has revision "2"
    When we reset fresh as "reset_fresh"
    Then "reset_fresh" result is "3"
    Then global revision is "3"
    And in changelog there is
      | revision | type        |
      | 3        | fresh-reset |

  Scenario: Write to changelog with request_info default to null
    Given new initialized user
    When we store "$1" into "inbox"
    And we reset fresh as "reset_fresh"
    Then "reset_fresh" result is "3"
    And in changelog there is
      | revision | type        | x_request_id | session_key |
      | 3        | fresh-reset |              |             |

  Scenario: Write to changelog with request_info
    Given new initialized user
    When we store "$1" into "inbox"
    And we set request_info "(x-request-id,session-key)"
    And we reset fresh as "reset_fresh"
    Then "reset_fresh" result is "3"
    And in changelog there is
      | revision | type        | x_request_id | session_key |
      | 3        | fresh-reset | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Reset fresh writes useful_new_count to changelog
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we reset fresh
    Then "fresh-reset" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Reset fresh does not touch other users
    Given new initialized user with "$1" in "inbox"
    And replication stream
    When we reset fresh
    Then there are only our user changes in replication stream
