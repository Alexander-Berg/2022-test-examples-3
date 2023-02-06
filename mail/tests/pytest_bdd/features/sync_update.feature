Feature: code.sync_update


  Background: Owner and subscriber
    Given new initialized user "Postgre" with "$1, $2, $3, $4" in "inbox" shared folder
    And new initialized user "Tom" with "hackers" subscribed to "inbox@Postgre"


  Scenario: sync_update_messages function updates flags on single synced message
    When we sync "$1" from "inbox@Postgre" new message "$1h" appears
    Then global revision is "4"
    And in folder named "hackers" there is one message
      | mid | revision | flags  |
      | $1h | 4        | recent |
    When we sync set "+seen, -recent, +deleted" on "$1" belonging to "inbox@Postgre"
    Then in folder named "hackers" there is one message
      | mid | revision | flags         |
      | $1h | 5        | seen, deleted |
    And global revision is "5"


  Scenario: sync adding one label on single message
    When we sync "$1" from "inbox@Postgre" new message "$1h" appears
    And  user "Postgre" sets "+system:pinned" on "$1"
    And we sync set "+system:pinned" on "$1" belonging to "inbox@Postgre"
    Then in folder named "hackers" there is one message
      | mid | label         |
      | $1h | system:pinned |
    And "system" label "pinned" has one message at revision "5"


  @changelog @changed
  Scenario: sync_update_messages function writes to changelog
    When we sync "$1" from "inbox@Postgre"
    And we sync set "+seen" on "$1" belonging to "inbox@Postgre"
    Then "sync-update" is last changelog entry
    And last changelog.changed matches "changed/sync_update_messages.json" schema
    And last changelog.arguments matches "arguments/update_messages.json" schema


  Scenario: sync_update_messages idempotency
    When we sync "$1" from "inbox@Postgre"
    Then global revision is "4"
    When we sync set "+seen" on "$1" belonging to "inbox@Postgre"
    Then global revision is "5"
    When we try sync set "+seen" on "$1" belonging to "inbox@Postgre" as "$update"
    And we commit "$update"
    Then "$update" result has unchanged revision
    And global revision is "5"


  Scenario: sync_update_messages updates synced_revision in subscribed_folders
    When we sync "$1" from "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "3"
    When user "Postgre" sets "+seen" on "$1"
    And we sync set "+seen" on "$1" belonging to "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "7"

  @other-user
  Scenario: Sync-update do not touch other users
    Given replication stream
    When we sync "$1" from "inbox@Postgre"
    Then there are only our user changes in replication stream
