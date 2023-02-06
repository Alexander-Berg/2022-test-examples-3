Feature: code.sync_message

  Scenario: Sync message write to synced_messages
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
  #  init=1, add_to_shared_folder=2, store_message=3
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
  #  init=1, create_folder=2, add_to_subscribed=3
    Then global revision is "3"
    When we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then there is one synced message
      | mid | revision | owner_mid | owner_revision |
      | $1r | 4        | $1r       | 3              |


  Scenario: Sync message set synced revision for subscribed folder and actual subscriber revision
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre"
    Then subscribed folder "hackers" has synced revision "3" and revision "4"

  Scenario: Sync message does not set synced revision if new revision is less than old
    When we initialize new user "Postgre" with "$1, $2" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$2" from "inbox@Postgre" new message "$1r" appears
    Then there is one synced message
      | mid | revision | owner_mid | owner_revision |
      | $1r | 4        | $2        | 4              |
    And subscribed folder "hackers" has synced revision "4" and revision "4"
    When we sync "$1" from "inbox@Postgre" new message "$2r" appears
    Then there are "2" synced messages
      | mid | revision | owner_mid | owner_revision |
      | $1r | 4        | $2        | 4              |
      | $2r | 5        | $1        | 3              |
    And subscribed folder "hackers" has synced revision "4" and revision "5"

  Scenario: Sync message write to changelog with sync-store type
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre"
    Then "sync-store" is last changelog entry


  @changelog @changed
  Scenario: Sync message writes valid changed to changelog
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre"
    Then last changelog.changed matches "changed/sync_store_messages.json" schema


  Scenario: Sync message increment subscriber revision
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
  # cause init=1, create-folder=2, add-subscribed-folder=3
    Then global revision is "3"
    When we sync "$1" from "inbox@Postgre"
    Then global revision is "4"


  Scenario: Quiet sync message writes quiet flag to changelog
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we quietly sync "$1" from "inbox@Postgre"
    Then in changelog there is
      | revision | type       | quiet |
      | 4        | sync-store | true  |


  Scenario: Sync message idempotency
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Heikki" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then global revision is "4"
    When we sync "$1" from "inbox@Postgre" new message "$1rc" appears
    Then global revision is "4"
    And "$1rc" is same result as "$1r"


  Scenario: Sync message should produce similar threads layout
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store into "inbox"
      | mid | tid |
      | $1  | 100 |
      | $2  | 200 |
      | $3  | 100 |
    And we initialize new user "Bruce" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1, $2, $3" from "inbox@Postgre" new messages "$1r, $2r, $3r" appear
    Then in folder named "hackers" there are "3" messages
      | mid | tid |
      | $1r | $1r |
      | $2r | $2r |
      | $3r | $1r |


  Scenario: Sync message add synced to attributes, and set synced thread_rule
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Michael" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then in folder named "hackers" there is one message
      | mid | attributes | thread_rule |
      | $1r | synced     | synced      |


  Scenario: Sync message with `synced` message.attribute
    When we initialize new user "Postgre" with "inbox" shared folder
    And we store "$1" into "inbox"
      | attributes |
      | synced     |
    And we initialize new user "Oleg" with "hackers" subscribed to "inbox@Postgre"
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then in folder named "hackers" there is one message
      | mid | attributes |
      | $1r | synced     |


  Scenario: Sync message save same headers info
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Vladimir" with "bugs" subscribed to "inbox@Postgre"
    And "Postgre" comeback
    And we store into "inbox"
      | mid | subject            | firstline                   | hdr_message_id          |
      | $1  | Weird generic plan | We’ve met a strange problem | FFBAC91E@yandex-team.ru |
    When "Vladimir" comeback
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then in folder named "bugs" there is one message
      | mid | subject            | firstline                   | hdr_message_id          |
      | $1r | Weird generic plan | We’ve met a strange problem | FFBAC91E@yandex-team.ru |


  Scenario: Sync message with attaches
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Andres" with "hackers" subscribed to "inbox@Postgre"
    And "Postgre" comeback
    And we store into "inbox"
      | mid | subject                        | attaches                        |
      | $1  | Improving executor performance | 1.2:text/x-patch:WIP.patch:4298 |
    And "Andres" comeback
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then in folder named "hackers" there is one message
      | mid | subject                        | attaches                        |
      | $1r | Improving executor performance | 1.2:text/x-patch:WIP.patch:4298 |


  Scenario: Sync message with mime
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Andres" with "hackers" subscribed to "inbox@Postgre"
    And "Postgre" comeback
    And we store into "inbox"
      | mid | mime                                               |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:300 |
    And "Andres" comeback
    And we sync "$1" from "inbox@Postgre" new message "$1r" appears
    Then in folder named "hackers" there is one message
      | mid | mime                                               |
      | $1r | 1:multipart:mixed:--boundary::UTF8:binary::::0:300 |


  Scenario: Sync message to user without subscribed folders
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Tom Kyte"
    And we try sync "$1" from "inbox@Postgre" as "$op"
    Then commit "$op" should produce "UserDontHaveSubscribedFolder"


  Scenario: Sync same owner_mid from different folders
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    Given he has "archive" shared folder
    When we initialize new user "Andres" with "hackers" subscribed to "inbox@Postgre"
    Given he has "hackers_archive" subscribed to "archive@Postgre"
    When we sync "$1" from "inbox@Postgre" new message "$1r_inbox" appears
    And "Postgre" comeback
    And we move "$1" to "archive"
    And "Andres" comeback
    And we sync "$1" from "archive@Postgre" new message "$1r_archive" appears
    Then there are "2" synced messages
      | owner_mid | mid         |
      | $1        | $1r_inbox   |
      | $1        | $1r_archive |


  @concurrent
  Scenario: Sync same message concurrently
    When we initialize new user "Postgre" with "$1" in "inbox" shared folder
    And we initialize new user "Heikki" with "hackers" subscribed to "inbox@Postgre"
    And we try sync "$1" from "inbox@Postgre" as "$sync"
    And we try sync "$1" from "inbox@Postgre" as "$concurrent-sync"
    And we commit "$sync"
    And we commit "$concurrent-sync"
    Then in folder named "hackers" there is one message
    And "$sync" is same result as "$concurrent-sync"
