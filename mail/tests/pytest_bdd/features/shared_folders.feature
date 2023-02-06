Feature: Shared folders management

  Scenario: Add folder to shared_folders
    When we initialize new user
    And we add "inbox" to shared folders
    Then "inbox" is shared folder

  @other-user
  Scenario: Add folder to shared_folders touch only shared folder user
    When we initialize new user
    And we setup replication stream
    When we add "inbox" to shared folders
    Then there are only our user changes in replication stream

  Scenario: New shared folder has 0 subscribers
    When we initialize new user
    And we add "inbox" to shared folders
    Then "inbox" has "0" subscribers

  Scenario: Add folder to shared_folders write to changelog
    When we initialize new user
    And we add "inbox" to shared folders
    Then "shared-folder-create" is last changelog entry

  Scenario: Add folder to shared_folders increment revision
    When we initialize new user
    Then global revision is "1"
    When we add "inbox" to shared folders
    Then global revision is "2"

  Scenario: Add folder to subscribed_folders
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    And we add "inbox@Postgre" as "hackers" to subscribed folders
    Then folder "hackers" is subscribed to "inbox@Postgre"

  Scenario: Remove folder from subscribed_folders
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "hackers" subscribed to "inbox@Postgre"
    And delete folder "hackers" from subscribed
    Then folder "hackers" is not subscribed for shared folder

  Scenario: Remove folder from subscribed_folders write to changelog
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "hackers" subscribed to "inbox@Postgre"
    And delete folder "hackers" from subscribed
    Then "subscribed-folder-delete" is last changelog entry

  @other-user
  Scenario: Add folder to subscribed_folders touch only subscriber
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Tom"
    And we create "user" folder "hackers"
    And we setup replication stream
    And we add "inbox@Postgre" as "hackers" to subscribed folders
    Then there are only "Tom" changes in replication stream

  Scenario: Remove folder from subscribed_folders touch only subscriber
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Tom" with "hackers" subscribed to "inbox@Postgre"
    And we setup replication stream
    And delete folder "hackers" from subscribed
    Then there are only "Tom" changes in replication stream

  Scenario: Currently added subscribed folder has unsynced revision (0)
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    And we add "inbox@Postgre" as "hackers" to subscribed folders
    Then subscribed folder "hackers" has synced revision "0"

  Scenario: Add folder to subscribed_folders write to changelog
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    And we add "inbox@Postgre" as "hackers" to subscribed folders
    Then "subscribed-folder-create" is last changelog entry

  Scenario: Add folder to subscribed_folder increment revision
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    Then global revision is "2"
    When we add "inbox@Postgre" as "hackers" to subscribed folders
    Then global revision is "3"

  Scenario: Remove folder from subscribed_folders increment revision
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "hackers" subscribed to "inbox@Postgre"
    Then global revision is "3"
    When delete folder "hackers" from subscribed
    Then global revision is "4"

  Scenario: Add folder to subscribed_folder idempotency check that revision is unchanged
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    And we add "inbox@Postgre" as "hackers" to subscribed folders
  # user_init + create_user_folder + create_subcribed_folder
    Then global revision is "3"
    When we add "inbox@Postgre" as "hackers" to subscribed folders
    Then global revision is "3"

  Scenario: Each folder can be subscribed to only one shared folder
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user "Oracle" with "drafts" shared folder
    And we initialize new user with "hackers" subscribed to "inbox@Postgre"
    And we try add "drafts@Oracle" as "hackers" to subscribed folders as "$op"
    Then commit "$op" should produce "FolderAlreadySubscribedToDifferentSharedFolder"

  Scenario: User can have only one subscription to same shared_folder
    When we initialize new user "Postgre" with "inbox" shared folder
    And we initialize new user with "user" folder "hackers"
    And we create "user" folder "release"
    And we add "inbox@Postgre" as "hackers" to subscribed folders
    And we try add "inbox@Postgre" as "release" to subscribed folders as "$op"
    Then commit "$op" should produce "SharedFolderAlreadySubscribed"

  Scenario: Add subscriber to shared folder
    When we initialize new user "Vladimir"
    And we initialize new user "Postgre" with "inbox" shared folder
    And we add "Vladimir" to "inbox@Postgre" subscribers
    Then "inbox" has "1" subscriber
    And "Vladimir" is "inbox" subscriber

  Scenario: Add subscriber write to changelog
    When we initialize new user "Vladimir"
    And we initialize new user "Postgre" with "inbox" shared folder
    And we add "Vladimir" to "inbox@Postgre" subscribers
    Then "shared-folder-subscribe" is last changelog entry

  Scenario: Add subscriber increment revision
    When we initialize new user "Vladimir"
    And we initialize new user "Postgre" with "inbox" shared folder
    Then global revision is "2"
    When we add "Vladimir" to "inbox@Postgre" subscribers
    Then global revision is "3"

  Scenario: Add subscriber idempotency
    When we initialize new user "Vladimir"
    When we initialize new user "Postgre" with "inbox" shared folder
    When we add "Vladimir" to "inbox@Postgre" subscribers
    Then global revision is "3"
    When we add "Vladimir" to "inbox@Postgre" subscribers
    Then global revision is "3"

  @other-user
  Scenario: Add subscriber touch only owner data
    When we initialize new user "Vladimir"
    When we initialize new user "Postgre" with "inbox" shared folder
    And we setup replication stream
    When we add "Vladimir" to "inbox@Postgre" subscribers
    Then there are only "Postgre" changes in replication stream
