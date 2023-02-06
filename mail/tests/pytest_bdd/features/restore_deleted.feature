Feature: [MAILPG-570] Restore deleted messages

  As a DBA
  I can restore deleted message.

  Scenario: Restore deleted message
    Given new initialized user
    When we store into "inbox"
      | mid | tid | subject               | hdr_message_id |
      | $1  | 10  | Your ticket to Brazil | <msg@lh.de>    |
    And he delete "$1"
    Then "inbox" is empty
    When DBA restore "$1" into "inbox" as "$restore"
    And "$restore" result produce one message "$1r"
    Then in "inbox" there is one message
      | mid | tid | subject               | hdr_message_id |
      | $1r | 10  | Your ticket to Brazil | <msg@lh.de>    |

  Scenario: Restore deleted message with mime
    Given new initialized user
    When we store into "inbox"
      | mid | mime                                               |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:200 |
    And he delete "$1"
    Then "inbox" is empty
    When DBA restore "$1" into "inbox" as "$restore"
    And "$restore" result produce one message "$1r"
    Then in "inbox" there is one message
      | mid | mime                                               |
      | $1r | 1:multipart:mixed:--boundary::UTF8:binary::::0:200 |

  Scenario: Restore pruned or non existent message
    Given new initialized user
    And non existent mid "$undefined"
    When DBA restore "$undefined" into "inbox" as "$restore"
    Then "$restore" result has one row with
         """
            why_not_restored: No data found in messages or delete_box
            restored_mid: null
         """
