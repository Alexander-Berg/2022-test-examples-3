Feature: Copy messages

  Scenario Outline: [MAILPG-238] Copy message with references and in_reply_to
    Given new initialized user
    When we store "$1" into "inbox"
      | references   | in_reply_to   |
      | <references> | <in_reply_to> |
    And we copy "$1" to "drafts" new message "$1c" appears
    Then in "drafts" there is one message
    And message "$1c" has "<count>" references
    Examples:
      | references | in_reply_to | count |
      | r1, r2     |             | 2     |
      | r1, r2     | r3          | 3     |
      | r1, r2     | r1          | 2     |
      | r1, r2     | r2          | 2     |

  @MAILPG-932 @useful_new_count
  Scenario: Copy message should write useful_new_count
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we copy "$1" to "trash" new message "$1c" appears
    Then "copy" is last changelog entry with "2" as useful_new_count

  @MAILDEV-800
  Scenario: Copy messages should save mime
    Given new initialized user
    When we store "$1" into "inbox"
      | mime                                               |
      | 1:multipart:mixed:--boundary::UTF8:binary::::0:100 |
    And we copy "$1" to "drafts" new message "$2" appears
    Then in "drafts" there is one message
      | mid | mime                                               | attributes | imap_id |
      | $2  | 1:multipart:mixed:--boundary::UTF8:binary::::0:100 | copy       | 1       |
