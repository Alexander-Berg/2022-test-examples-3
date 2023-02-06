Feature: code.sync_join_threads


  Scenario: sync_join_threads updates subscriber's folder
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $2r |
      | $3r | $3r |
      | $4r | $2r |
    When we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
      | $3r | $1r |
      | $4r | $1r |


  Scenario: sync_join_threads does not affect other threads
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 400 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $2r |
      | $3r | $3r |
      | $4r | $4r |
    When we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
      | $3r | $1r |
      | $4r | $4r |


  Scenario: sync_join_threads updates synced_messages
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 100 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    Then there are "4" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $1r |
      | $2r | $2        | 200       | $2r |
      | $3r | $3        | 300       | $3r |
      | $4r | $4        | 100       | $1r |
    When we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then there are "4" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $1r |
      | $2r | $2        | 100       | $1r |
      | $3r | $3        | 100       | $1r |
      | $4r | $4        | 100       | $1r |


  Scenario: sync_join_threads writes valid record to changelog
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2" from "inbox@Postgre" new messages "$1r, $2r" appear
    And we sync joined "200" into "100" belonging to "inbox@Postgre"
    Then "sync-threads-join" is last changelog entry
    And last changelog.changed matches "changed/sync_join_threads.json" schema
    And last changelog.arguments matches "arguments/sync_join_threads.json" schema


  Scenario: sync_join_threads updates synced revision in subscribed_folders
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2" from "inbox@Postgre" new messages "$1r, $2r" appear
    Then subscribed folder "hackers" has synced revision "4"
    When user "Postgre" joins "200" into "100"
    And we sync joined "200" into "100" belonging to "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "5"


  @other-user
  Scenario: sync_join_threads do not touch other users
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2" from "inbox@Postgre" new messages "$1r, $2r" appear
    And user "Postgre" joins "200" into "100"
    And we setup replication stream
    And we sync joined "200" into "100" belonging to "inbox@Postgre"
    Then there are only "Bruce" changes in replication stream


  @MAILPG-1794
  Scenario: sync_join_threads does not fail if thread was already joined
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2" from "inbox@Postgre" new messages "$1r, $2r" appear
    And we store "$3r" into "inbox" with "$2r"s thread
    And we join "$2r"s thread into "$1r"s thread
    Then in folder named "hackers" there are "2" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
    And in "inbox" there is one message
      | mid | tid |
      | $3r | $1r |
    When we sync joined "200" into "100" belonging to "inbox@Postgre"
    Then there are "2" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $1r |
      | $2r | $2        | 100       | $1r |

  @MAILPG-1992
  Scenario: sync_join_threads merge threads successfully even if one of the threads deleted by user
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    And we move "$3r" to "trash"
    Then in folder named "hackers" there are "3" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $2r |
      | $4r | $2r |
    When we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then in folder named "hackers" there are "3" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
      | $4r | $1r |
    And there are "4" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $1r |
      | $2r | $2        | 100       | $1r |
      | $3r | $3        | 100       | $1r |
      | $4r | $4        | 100       | $1r |
    When we move "$3r" to "hackers"
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
      | $3r | $1r |
      | $4r | $1r |

  Scenario: sync_join_threads merge threads successfully even if destination thread deleted by user
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    And we move "$1r" to "trash"
    And we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then in folder named "hackers" there are "3" messages
      | mid | tid |
      | $2r | $2r |
      | $3r | $2r |
      | $4r | $2r |
    And there are "4" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $2r |
      | $2r | $2        | 100       | $2r |
      | $3r | $3        | 100       | $2r |
      | $4r | $4        | 100       | $2r |
    When we move "$1r" to "hackers"
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $2r |
      | $2r | $2r |
      | $3r | $2r |
      | $4r | $2r |

  Scenario: sync_join_threads merge threads successfully if all of the threads deleted by user
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 300 |
      | $4  | 200 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3, $4" from "inbox@Postgre" new messages "$1r, $2r, $3r, $4r" appear
    And we move "$1r, $2r, $3r, $4r" to "trash"
    And we sync joined "200, 300" into "100" belonging to "inbox@Postgre"
    Then in folder named "hackers" there are "0" messages
    And there are "4" synced messages
      | mid | owner_mid | owner_tid | tid |
      | $1r | $1        | 100       | $1r |
      | $2r | $2        | 100       | $1r |
      | $3r | $3        | 100       | $1r |
      | $4r | $4        | 100       | $1r |
    When we move "$1r, $2r, $3r, $4r" to "hackers"
    Then in folder named "hackers" there are "4" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $1r |
      | $3r | $1r |
      | $4r | $1r |