Feature: Clone user [MAILPG-630]

  Scenario: Clone user into unregistered user with pg db
    Given new user "Chuck"
    And new unregistered user "Grace" with pg db in blackbox
    When we clone "Chuck" into "Grace"
    Then there is our "clone_user" task
    And task is successful
    And clone "Chuck" into "Grace" is completed
    And "Grace" has same mails as "Chuck" except st_ids and user state are different

  Scenario: Clone into previously registered user should fail
    When we make new user "Trent"
    And we make new user "Bruce"
    Given "Bruce" was inited "5" days ago
    When we clone "Trent" into "Bruce"
    Then there is our "clone_user" task
    Then task status is "error"
    And error is "not_supported"
    And clone "Trent" into "Bruce" is not completed

  @force
  Scenario: Clone user with force option should success
    When we make new user "Trent"
    And we make new user "Bruce"
    Given "Bruce" was inited "5" days ago
    When we clone "Trent" into "Bruce"
    Then there is our "clone_user" task
    Then task status is "error"
    When we make our task "pending" with addition task args
      """
      {"force": true}
      """
    Then task is successful
    And clone "Trent" into "Bruce" is completed

  Scenario: Clone into just registered user should succeed
    When we make new user "Vasya"
    And we make new user "Petya"
    And we clone "Vasya" into "Petya"
    Then there is our "clone_user" task
    And task is successful
    And clone "Vasya" into "Petya" is completed
    And "Petya" has same mails as "Vasya" except st_ids and user state are different

  @MAILPG-1006
  Scenario: Clone into user registered in sharpei but without mdb meta
    When we make new user "Alice"
    Given new user "Mallory" uninitialized in mdb
    When we clone "Alice" into "Mallory"
    And our "clone_user" task is completed
    Then clone "Alice" into "Mallory" is completed

  Scenario: Clone user should disable unsafe filters
    When we make new user "Alice"
    Given "Alice" has enabled filter "forward" to "Bob"
    When we make new user "Mallory"
    And we clone "Alice" into "Mallory"
    And our "clone_user" task is completed
    Then "Mallory" has disabled filter "forward" to "Bob"

  @MAILPG-1779
  Scenario: Clone user with deleted messages
    Given new user "Huey" with deleted messages in first shard
    And new user "Dewey"
    When we clone "Huey" into "Dewey"
    Then there is our "clone_user" task
    And task is successful
    And clone "Huey" into "Dewey" is completed
    And "Dewey" has same mails as "Huey" except st_ids and user state are different

  @MAILDEV-1423
  Scenario: Clone user with restore deleted messages
    When we make new user "Robert"
    Given "Robert" has metadata named "metaRobert"
    And he delete some messages from inbox
    When we make new user "Duncan"
    And we clone "Robert" into "Duncan" with restore deleted messages
    Then there is our "clone_user" task
    And task is successful
    And clone "Robert" into "Duncan" is completed
    And "Duncan" has metadata named "metaDuncan"
    And metadata "metaDuncan" contains folder "deleted_box"
    And metadata "metaDuncan" and "metaRobert" have same mails except stids, lids and seen flags

  @MAILPG-3344
  Scenario: Call clone user with specified date interval when all mails in this interval
    Given new empty user "Miguel"
    And user has "30" messages
    And "30" messages have received_date "2020-02-02T00:00:00+03:00"
    When we make new user "Malcolm"
    When we clone "Miguel" into "Malcolm" with params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Miguel" into "Malcolm" is completed
    And "Malcolm" has same mails as "Miguel" except st_ids and user state are different

  @MAILPG-3344
  Scenario: Call clone user with specified date interval when no mails in this interval
    Given new empty user "Reginald"
    And user has "30" messages
    And "30" messages have received_date "2020-01-02T00:00:00+03:00"
    When we make new user "Rodrigo"
    When we clone "Reginald" into "Rodrigo" with params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Reginald" into "Rodrigo" is completed
    And "Rodrigo" has empty mails, mailish and shared info

  @MAILPG-3344
  Scenario: Call clone user with specified date interval when some mails in this interval
    Given new empty user "Sebastian"
    And user has "30" messages
    And "22" messages have received_date "2020-02-02T00:00:00+03:00"
    When we make new user "Stanley"
    When we clone "Sebastian" into "Stanley" with params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Sebastian" into "Stanley" is completed
    And "Stanley" has 22 mails

  @MAILPG-3344
  Scenario: Call clone user with bad task arguments
    Given new empty user "Logan"
    And user has "30" messages
    And "30" messages have received_date "2020-01-02T00:00:00+03:00"
    When we make new user "Luke"
    When we plan clone "Logan" into "Luke" with params
      """
      {
        "min_rcvd_date": "2020-02-01T00:00:00+03:00",
        "max_rcvd_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then response status is error with reason "unknown task argument"

  @MAILPG-3346
  Scenario: Call clone user with specified date interval and restore deleted messages when all mails in this interval
    Given new empty user "Jeremiah"
    And user has "30" messages
    And "30" messages have received_date "2020-02-02T00:00:00+03:00"
    And "Jeremiah" has metadata named "metaJeremiah"
    And he delete some messages from inbox
    When we make new user "Joshua"
    And we clone "Jeremiah" into "Joshua" with restore deleted messages and params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Jeremiah" into "Joshua" is completed
    And "Joshua" has metadata named "metaJoshua"
    And metadata "metaJoshua" contains folder "deleted_box"
    And metadata "metaJoshua" and "metaJeremiah" have same mails except stids, lids and seen flags

  @MAILPG-3346
  Scenario: Call clone user with specified date interval and restore deleted messages when some mails in this interval
    Given new empty user "Clifford"
    And user has "30" messages
    And "21" messages have received_date "2020-02-02T00:00:00+03:00"
    And he delete some messages from inbox
    When we make new user "Curtis"
    And we clone "Clifford" into "Curtis" with restore deleted messages and params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Clifford" into "Curtis" is completed
    And "Curtis" has 21 mails

  @MAILPG-3346
  Scenario: Call clone user with specified date interval and restore deleted messages when no mails in this interval
    Given new empty user "Jeffery"
    And user has "30" messages
    And "30" messages have received_date "2020-01-02T00:00:00+03:00"
    And he delete some messages from inbox
    When we make new user "Jaden"
    And we clone "Jeffery" into "Jaden" with restore deleted messages and params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Jeffery" into "Jaden" is completed
    And "Jaden" has empty mails, mailish and shared info

  @MAILPG-3490
  Scenario: Call clone user with specified date interval when some mails with label in this interval
    Given new empty user "Timothy"
    And user has "30" messages
    And user has label "test"
    And his messages have label "test"
    And "3" messages have received_date "2020-02-02T00:00:00+03:00"
    When we make new user "Chase"
    When we clone "Timothy" into "Chase" with params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "Timothy" into "Chase" is completed
    And "Chase" has 3 mails

  Scenario: Clone user with backups
    Given new user "HueyWithBackups"
    And folders with types "inbox" are in backup settings
    And user has filled backup
    And user has restore
    When we make new user "HueyWithBackupsClone"
    And we clone "HueyWithBackups" into "HueyWithBackupsClone"
    Then there is our "clone_user" task
    And task is successful
    And clone "HueyWithBackups" into "HueyWithBackupsClone" is completed
    And "HueyWithBackupsClone" has same metadata as "HueyWithBackups" except st_ids, state and backups
    And "HueyWithBackupsClone" has empty backups

  @MAILPG-4126
  Scenario: Clone user with mails in hidden_trash
    Given new empty user "HueyWithHiddenTrash"
    And user has folder "hidden_trash" with symbol "hidden_trash"
    And user has "5" messages in "hidden_trash"
    And new user "HueyWithHiddenTrashClone"
    When we clone "HueyWithHiddenTrash" into "HueyWithHiddenTrashClone"
    Then there is our "clone_user" task
    And task is successful
    And clone "HueyWithHiddenTrash" into "HueyWithHiddenTrashClone" is completed
    And "HueyWithHiddenTrashClone" has same mails as "HueyWithHiddenTrash" except st_ids and user state are different

  @MAILPG-4126
  Scenario: Call clone user with specified date interval and restore deleted messages when all mails in hidden_trash in this interval
    Given new empty user "JeremiahWithHiddenTrash"
    And user has folder "hidden_trash" with symbol "hidden_trash"
    And user has "5" messages in "hidden_trash"
    And "5" messages have received_date "2020-02-02T00:00:00+03:00"
    And "JeremiahWithHiddenTrash" has metadata named "metaJeremiahWithHiddenTrash"
    When we make new user "JeremiahWithHiddenTrashClone"
    And we clone "JeremiahWithHiddenTrash" into "JeremiahWithHiddenTrashClone" with restore deleted messages and params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "JeremiahWithHiddenTrash" into "JeremiahWithHiddenTrashClone" is completed
    And "JeremiahWithHiddenTrashClone" has metadata named "metaJeremiahWithHiddenTrashClone"
    And metadata "metaJeremiahWithHiddenTrashClone" and "metaJeremiahWithHiddenTrash" have same mails except metadata
    And metadata "metaJeremiahWithHiddenTrashClone" contains folder "deleted_box"
    And "JeremiahWithHiddenTrashClone" has "5" mails in folder "deleted_box"
    And "JeremiahWithHiddenTrashClone" has "0" mails in folder "hidden_trash"

  @MAILPG-4126
  Scenario: Call clone user with specified date interval and restore deleted messages when all mails in hidden_trash not in this interval
    Given new empty user "TimothyWithHiddenTrash"
    And user has folder "hidden_trash" with symbol "hidden_trash"
    And user has "5" messages in "hidden_trash"
    And "5" messages have received_date "2020-01-02T00:00:00+03:00"
    When we make new user "TimothyWithHiddenTrashClone"
    And we clone "TimothyWithHiddenTrash" into "TimothyWithHiddenTrashClone" with restore deleted messages and params
      """
      {
        "min_received_date": "2020-02-01T00:00:00+03:00",
        "max_received_date": "2020-03-01T00:00:00+03:00"
      }
      """
    Then there is our "clone_user" task
    And task is successful
    And clone "TimothyWithHiddenTrash" into "TimothyWithHiddenTrashClone" is completed
    And "TimothyWithHiddenTrashClone" has empty mails, mailish and shared info

  @MAILPG-4196
  Scenario: Clone non-archived user should change user state to special
    When we make new user "BruceFrozen"
    And we make new user "BruceFrozenClone"
    Given "BruceFrozen" is in "frozen" state
    When we clone "BruceFrozen" into "BruceFrozenClone"
    Then there is our "clone_user" task
    And task is successful
    And clone "BruceFrozen" into "BruceFrozenClone" is completed
    And "BruceFrozen" has same mails as "BruceFrozenClone" except st_ids and user state are different
    And "BruceFrozenClone" is in "special" state

  @MAILPG-4196
  Scenario: Clone archived user should fail
    When we make new user "BruceArchived"
    And we make new user "BruceArchivedClone"
    Given "BruceArchived" is in "archived" state
    When we clone "BruceArchived" into "BruceArchivedClone"
    Then there is our "clone_user" task
    Then task status is "error"
    And error is "not_supported"
    And clone "BruceArchived" into "BruceArchivedClone" is not completed
  
  @MAILPG-4502
  Scenario: Clone user with reply later messages
    Given new empty user "HueyWithReplyLaterMessages"
    And user has folder "reply_later" with symbol "reply_later"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "reply_later"
      | mid | lids      |
      | $1  | $news_lid |
      | $2  |           |
      | $3  |           |
    And user has reply later stickers for messages
      | mid |
      | $1  |
      | $2  |
    And new user "HueyWithReplyLaterMessagesClone"
    When we clone "HueyWithReplyLaterMessages" into "HueyWithReplyLaterMessagesClone"
    Then there is our "clone_user" task
    And task is successful
    And clone "HueyWithReplyLaterMessages" into "HueyWithReplyLaterMessagesClone" is completed
    And "HueyWithReplyLaterMessagesClone" has same mails as "HueyWithReplyLaterMessages" except st_ids and user state are different
