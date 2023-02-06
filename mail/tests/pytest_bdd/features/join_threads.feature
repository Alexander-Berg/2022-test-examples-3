Feature: Concurrent messages update

  Scenario Outline: Join 2 threads from inbox
    Given new initialized user with apple and banana labels
    When we store into "inbox"
      | mid | tid | flags | labels                 |
      | $1  | 10  | seen  | user:apple             |
      | $2  | 20  | seen  |                        |
      | $3  | 20  |       | user:apple,user:banana |
    Then "inbox" has "3" messages, "1" unseen at revision "6"
    Then in "inbox" there are "3" messages
      | mid | tid | flags        | labels                 | revision |
      | $1  | 10  | seen, recent | user:apple             | 4        |
      | $2  | 20  | seen, recent |                        | 5        |
      | $3  | 20  | recent       | user:apple,user:banana | 6        |
    And "user" label "apple" has "2" messages at revision "6"
    And "user" label "banana" has one message at revision "6"
    And in "inbox" there are "2" threads
      | tid | count | unseen |
      | 10  | 1     | 0      |
      | 20  | 2     | 1      |
    When we join "<join_tid>" into "<tid>"
    Then "inbox" has "3" messages, "1" unseen at revision "7"
    And in "inbox" there are "3" messages
      | mid | tid   | flags        | labels                 | revision |
      | $1  | <tid> | seen, recent | user:apple             | <rev_1>  |
      | $2  | <tid> | seen, recent |                        | <rev_2>  |
      | $3  | <tid> | recent       | user:apple,user:banana | <rev_3>  |
    And in "inbox" there is one thread
      | tid   | count | unseen |
      | <tid> | 3     | 1      |
    And "user" label "apple" has "2" messages at revision "7"
    And "user" label "banana" has one message at revision "<rev_3>"
    Examples:
      | tid | join_tid | rev_1 | rev_2 | rev_3 |
      | 10  | 20       | 4     | 7     | 7     |
      | 20  | 10       | 7     | 5     | 6     |

  Scenario: Join threads from different folders
    Given new initialized user
    When we store messages
      | folder | mid | tid |
      | inbox  | $1  | 10  |
      | inbox  | $2  | 20  |
      | inbox  | $3  | 30  |
      | drafts | $4  | 10  |
      | drafts | $5  | 20  |
      | drafts | $6  | 30  |
      | sent   | $7  | 10  |
      | sent   | $8  | 20  |
      | sent   | $9  | 30  |
    Then global revision is "10"
    When we join "20, 30" into "10"
    Then in folders "inbox, drafts, sent" there is one thread
      | tid | count | revision |
      | 10  | 9     | 11       |

  Scenario: Join threads write changelog
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we join "200" into "100"
    Then global revision is "4"
    Then in changelog there is
      | revision | type         | x_request_id | session_key |
      | 4        | threads-join |              |             |

  Scenario: Join threads write changelog with request info
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we set request_info "(x-request-id,session-key)"
    And we join "200" into "100"
    Then global revision is "4"
    Then in changelog there is
      | revision | type         | x_request_id | session_key |
      | 4        | threads-join | x-request-id | session-key |

  Scenario Outline: Try join not existed threads
    Given new initialized user
    When we store "$1" into "inbox"
      | tid |
      | 42  |
    Then global revision is "2"
    When we try join "<join_tids>" into "<tid>" as "$broken_join"
    Then COMMIT "$broken_join" should produce "InvalidTIDsError"

    Examples:
      | join_tids | tid    |
      | 100500    | 42     |
      | 42        | 100500 |
      | 100500    | 100500 |
      | 42        | 42     |

  @MAILPG-932 @useful_new_count
  Scenario: Join threads write useful_new_count to changelog
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    And we store "$3" into "trash"
    When we join "10" into "20"
    Then "threads-join" is last changelog entry with "2" as useful_new_count

  @changelog @changed
  Scenario: Join threads writes valid changed to changelog
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    When we join "10" into "20"
    Then last changelog.changed matches "changed/update_messages.json" schema

  @other-user
  Scenario: Join threads do not touch other users
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    And we setup replication stream
    And we join "10" into "20"
    Then there are only our user changes in replication stream


  Scenario: Join threads with newer messages from same tab
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    When we join "20" into "10"
    Then in tab "relevant" there is "1" thread
      | tid | mid | count |
      | 10  | $2  | 2     |

  Scenario: Join threads with older messages from same tab
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    And we join "10" into "20"
    Then in tab "relevant" there is "1" thread
      | tid | mid | count |
      | 20  | $2  | 2     |

  Scenario: Join threads from different tabs
    Given new initialized user
    When we store into tab "news"
      | mid | tid |
      | $1  | 10  |
      | $2  | 20  |
    And we store into tab "social"
      | mid | tid |
      | $3  | 30  |
    And we join "30" into "10"
    Then in tab "news" there are "2" threads
      | tid | mid | count |
      | 10  | $1  | 2     |
      | 20  | $2  | 1     |
    And in tab "social" there is "1" thread
      | tid | mid | count |
      | 10  | $3  | 2     |
