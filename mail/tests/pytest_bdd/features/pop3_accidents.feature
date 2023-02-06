Feature: Workarounds for problems with POP3

  @MAILPG-797
  Scenario: Check if we can delete this message
    Given new user with popped "inbox"
    When we store "$[1:3]" into "sent"
    And "$3" message accidentally appears in pop3 box
    When we delete "$[1:3]"
    Then "sent" is empty
    And pop3 box is empty

  @MAILPG-797
  Scenario: Move messages to "inbox"
    Given new user with popped "inbox"
    When we store "$[1:3]" into "sent"
    And "$3" message accidentally appears in pop3 box
    When we move "$[1:3]" to "inbox"
    Then pop3 box is
      | mid | folder |
      | $1  | inbox  |
      | $2  | inbox  |
      | $3  | inbox  |

  @MAILPG-944
  Scenario: Initialize POP3
    Given new user with popped "inbox"
    When we store "$[1:3]" into "sent"
    And "$3" message accidentally appears in pop3 box with "inbox" folder
    When we initialize pop3 for "sent"
    Then pop3 box is
      | mid | folder |
      | $1  | sent   |
      | $2  | sent   |
      | $3  | sent   |

  @MAILPG-1227
  Scenario: POP3 delete idempotency
    Given new user with popped "inbox"
    When we store "$1" into "inbox"
    And we pop3-delete "$1"
    And we try pop3-delete "$1" as "$op"
    And we commit "$op"
    Then "$op" result has unchanged revision
