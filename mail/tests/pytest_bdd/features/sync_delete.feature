Feature: code.sync_delete

  Scenario: delete_synced_messages function deletes single synced message
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1h" appears
    Then global revision is "4"
    When we delete synced messages "$1" belonging to "inbox@Postgre"
    Then there are "0" synced messages
    And messages table is empty
    And storage delete queue is empty
    And folder named "hackers" is empty
    And global revision is "5"


  Scenario: delete_synced_messages function deletes multiple synced messages
    Given new initialized user "Postgre" with "$1, $2, $3" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1h" appears
    And we sync "$2" from "inbox@Postgre" new message "$2h" appears
    Then global revision is "5"
    When we delete synced messages "$1, $2" belonging to "inbox@Postgre"
    Then there are "0" synced messages
    And messages table is empty
    And storage delete queue is empty
    And folder named "hackers" is empty
    And global revision is "6"


  Scenario: delete_synced_messages function deletes synced messages from actual fid
    Given new initialized user "Postgre" with "$1, $2" in "inbox" shared folder
    And he has "archive" shared folder with "$3, $4"
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And he has "hackers_archive" subscribed to "archive@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1h" appears
    And we sync "$2" from "inbox@Postgre" new message "$2h" appears
    And we sync "$3" from "archive@Postgre" new message "$3ha" appears
    And we sync "$4" from "archive@Postgre" new message "$4ha" appears
    And we delete synced messages "$1, $2" belonging to "inbox@Postgre"
    Then there are "2" synced messages
      | mid  | owner_mid |
      | $3ha | $3        |
      | $4ha | $4        |
    And "$1h, $2h" are purged from messages
    And storage delete queue is empty
    And folder named "hackers" is empty
    And in folder named "hackers_archive" there are "2" messages
      | mid  |
      | $3ha |
      | $4ha |


  @changelog @changed
  Scenario: delete_synced_messages function writes to changelog
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre"
    And we delete synced messages "$1" belonging to "inbox@Postgre"
    Then "sync-delete" is last changelog entry
    And last changelog.changed matches "changed/sync_delete_messages.json" schema


  Scenario: delete_synced_messages idempotency
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre"
    Then global revision is "4"
    When we delete synced messages "$1" belonging to "inbox@Postgre"
    Then global revision is "5"
    When we try delete synced messages "$1" belonging to "inbox@Postgre" as "$delete"
    And we commit "$delete"
    Then "$delete" result has unchanged revision
    And global revision is "5"


  Scenario: delete_synced_messages updates synced_revision in subscribed_folders
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "3"
    When user "Postgre" deletes "$1"
    And we delete synced messages "$1" belonging to "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "4"


  Scenario: synced_messages has cascade cleanup
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1r" appears
    And we delete "$1r"
    Then there are "0" synced messages

  Scenario: delete synced from user inbox
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1s" appears
    And we move "$1s" to "inbox"
    Then "inbox" has one message
    When we delete synced messages "$1" belonging to "inbox@Postgre"
    Then "inbox" is empty

  @other-user
  Scenario: Sync-delete do not touch other users
    Given new initialized user "Postgre" with "$1" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1s" appears
    And we move "$1s" to "inbox"
    And we setup replication stream
    When we delete synced messages "$1" belonging to "inbox@Postgre"
    Then there are only our user changes in replication stream
