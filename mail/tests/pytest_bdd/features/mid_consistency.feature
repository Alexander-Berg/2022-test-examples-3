Feature: Mid generation algorithm consistency check
# This feature tests that almost all messages satisfy:
#   (mid[i] <= mid[i+1]) && (received_date[i] <= received_date[i+1])
# but it is not always possible because of mid generation algorithm.
# But in some places we depend on this algorithm.
# One of them is the process of archiving users.

  @MAILPG-4241
  Background: All on new initialized user
    Given new initialized user

  Scenario: Mid basic case
    When we store "$[1:7]" into "inbox"
    Then message "mid"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

  Scenario: Received date basic case
    When we store "$[1:7]" into "inbox"
    Then message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

  Scenario: Mid and received date case
    When we store "$[1:7]" into "inbox"
    Then message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

  Scenario: Append and check mid
    When we store "$[1:7]" into "inbox"
    And  we store "$10" into "inbox"
    Then message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

    When we store "$9" into "inbox"
    Then "inbox" has "9" messages
    And  message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

    When we store "$8" into "inbox"
    Then "inbox" has "10" messages
    And  message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid


  Scenario: Message from the past
    When we store "$[1:7]" into "inbox"
    And  we store "$8" into "inbox"
      | received_date           |
      | 1337-01-02 03:04:05 UTC |
    Then "inbox" has "8" messages
    And  message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

  Scenario: Messages from the past
      # Because of almost the same dates, but different by 1s
      # The order of mids is broken here: mids asc, date desc.
    When we store into "inbox"
      | mid | received_date           |
      | $1  | 2000-01-01 00:00:10 UTC |
      | $2  | 2000-01-01 00:00:9 UTC  |
      | $3  | 2000-01-01 00:00:8 UTC  |
      | $4  | 2000-01-01 00:00:7 UTC  |
      | $5  | 2000-01-01 00:00:6 UTC  |
      | $6  | 2000-01-01 00:00:5 UTC  |
    Then "inbox" has "6" messages
    And  message "mid"s in "inbox" are sorted in asc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid

  Scenario: Message from the future
    When we store "$[1:7]" into "inbox"
    And  we store "$8" into "inbox"
      | received_date           |
      | 2337-05-04 03:02:01 UTC |
    And  we store "$9" into "inbox"

    Then message "mid"s in "inbox" are sorted in desc order
    And  message "received_date"s in "inbox" are sorted in desc order
    And  last stored message has correct mid
