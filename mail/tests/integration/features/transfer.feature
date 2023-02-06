Feature: Transfer task tests

  Scenario: PG to PG transfer by shard id
    Given new user
    When we plan transfer
    Then task is successful

  Scenario: Transfer user to closed shard
    Given new user
    And "2" shard registration is closed
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2"
      }
      """
    Then task status is "error"
    And error is "closed_dest_shard"
    And there was 1 try
    Given "2" shard registration is opened

  Scenario: PG to PG transfer to shard name
    Given new user
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:shard2"
      }
      """
    Then transfer is successful

  Scenario: PG to PG transfer from shard name
    Given new user
    When we plan "transfer"
      """
      {
        "from_db": "postgre:shard1",
        "to_db": "postgre:2"
      }
      """
    Then transfer is successful

  Scenario: Duplicate user to other shard
    Given new user
    And he has metadata named "metaStart1" in shard "1"
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2",
        "duplicate": true
      }
      """
    Then transfer is successful
    And he has metadata named "metaEnd1" in shard "1"
    And metadata "metaStart1" and "metaEnd1" are equal
    And metadata "metaEnd1" user.is_here attribute is true
    And he has metadata named "metaEnd2" in shard "2"
    And metadata "metaStart1" and "metaEnd2" are equal
    And metadata "metaEnd2" user.is_here attribute is false

  Scenario: Transfer user to other shard in two steps(presync without ro and final sync with ro)
    Given new user
    And user has at least "10" messages
    And "5" messages have received_date "2019-01-01T00:00:00+03:00"
    And he has metadata named "metaStart1" in shard "1"
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2",
        "presync_received_date": "2020-01-01T00:00:00+03:00",
        "presync": true
      }
      """
    Then transfer is successful
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2",
        "presync_received_date": "2020-01-01T00:00:00+03:00",
        "sync": true
      }
      """
    Then transfer is successful
    And he has metadata named "metaEnd1" in shard "1"
    And metadata "metaStart1" and "metaEnd1" are equal
    And metadata "metaEnd1" user.is_here attribute is false
    And he has metadata named "metaEnd2" in shard "2"
    And metadata "metaStart1" and "metaEnd2" are equal
    And metadata "metaEnd2" user.is_here attribute is true

  Scenario: Transfer user absent in blackbox
    Given new user absent in blackbox
    When we plan transfer
    Then task status is "error"
    And error is "no_such_user"
    And there was 1 try

  Scenario: Transfer of a user with too many messages should be deferred
    Given new user
    And user has at least "10" messages
    When we set message count limit to "5"
    And we plan transfer
    Then task status is "error"
    And error is "deferred_due_to_message_count_limit"
    And there was 1 try

  Scenario: Message count check can be disabled for a user
    Given new user
    And user has at least "10" messages
    When we set message count limit to "5"
    And we plan transfer with disabled message count check
    Then transfer is successful

  Scenario: Transfer user in stoplist
    Given new user in stoplist
    When we plan transfer
    And wait for husky to poll tasks
    Then task status is "pending"

  Scenario: Transfer with change_tabs should change tabs
    Given new user
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
      | $2  |     |             |
      | $3  |     | $social_lid |
    When we plan "transfer"
      """
      {
        "from_db": "postgre:1",
        "to_db": "postgre:2",
        "change_tabs": true
      }
      """
    Then transfer is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  | relevant |
      | $3  | social   |
    And user can read tabs

   @MAILDEV-1717
   Scenario: Transfer deleted user
   When we make new user "Mulder"
   Given "Mulder" has metadata named "xfiles"
   And he delete some messages from inbox
   When we request delete user
   And we plan transfer with force flag
   Then there is our "transfer" task
   And task is successful
   And received date in deleted box has been transferred
   When we make new user "Scully"
   And we clone "Mulder" into "Scully" with restore deleted messages
   Then there is our "clone_user" task
   And task is successful
   And clone "Mulder" into "Scully" is completed
   And "Scully" has metadata named "area51"
   And metadata "area51" contains folder "deleted_box"
   And metadata "xfiles" and "area51" have same mails except metadata

  @MAILDEV-1717
  Scenario: Transfer user with storage delete queue
    Given new user with stids in storage delete queue
    When we plan transfer
    Then transfer is successful
    And storage delete queue is empty

  Scenario Outline: Transfer user with active concurrent operation(with restricted archivation state)
    When we make new user "<user>"
    Given "<user>" is in "<user_state>" state
    Given "<user>" is in "<archivation_state>" archivation state
    When we plan transfer
    Then task status is "error"
    And error is "user_blocked"
    And there was 1 try

    Examples:
    | user                  | user_state  | archivation_state         |
    | ArchivationInProgress | frozen      | archivation_in_progress   |
    | ArchivationError      | archived    | archivation_error         |
    | RestorationInProgress | active      | restoration_in_progress   |
    | RestorationError      | active      | restoration_error         |
    | RestorationComplete   | active      | restoration_complete      |


  Scenario Outline: Transfer user with acceptable archivation state
    When we make new user "<user>"
    Given "<user>" is in "<user_state>" state
    Given "<user>" is in "<archivation_state>" archivation state
    When we plan transfer
    Then transfer is successful

    Examples:
    | user                  | user_state  | archivation_state         |
    | ArchivationComplete   | archived    | archivation_complete      |
