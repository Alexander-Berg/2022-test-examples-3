Feature: Test IMAP unsubscribed folders

  Background: New user
    Given new initialized user

  Scenario: New user has Outbox unsubscribed
    Then there is one unsubscribed folder "{Outbox}" at revision "1"

  Scenario: Remove unsubscribed folder
    When we remove "{Outbox}" from unsubscribed
    Then there are no unsubscribed folders
    And in changelog there is
      | revision | type                     | x_request_id | session_key |
      | 2        | imap-delete-unsubscribed |              |             |

  Scenario: Add unsubscribed folder
    When we add "{Some,Strange,Name}" to unsubscribed
    Then there are "2" unsubscribed folders
      | full_name           | revision |
      | {Outbox}            | 1        |
      | {Some,Strange,Name} | 2        |
    And in changelog there is
      | revision | type                  | x_request_id | session_key |
      | 2        | imap-add-unsubscribed |              |             |

  Scenario Outline: Modify imap unsubscribed folders with request info
    When we set request_info "(some-id,some-key)"
    And we <unsubscribed_action>
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 2        | <change_type> | some-id      | some-key    |
    Examples:
      | unsubscribed_action                 | change_type              |
      | remove "{Outbox}" from unsubscribed | imap-delete-unsubscribed |
      | add "{Foo}" to unsubscribed         | imap-add-unsubscribed    |

  Scenario: Remove non exists full name
    When we remove "{Foo,Bar,Baz}" from unsubscribed
    Then global revision is "1"

  Scenario: Add existed full name
    When we add "{Outbox}" to unsubscribed
    Then global revision is "1"

  Scenario Outline: Try add broken full names
    When we try add "<full_name>" to unsubscribed as "$broken"
    Then commit "$broken" should produce "InvalidFolderFullName"

    Examples:
      | full_name  |
      | NULL       |
      | {}         |
      | {x,,z}     |
      | {x,NULL,z} |

  @MAILPG-932 @useful_new_count
  Scenario Outline: Imap unsubscribe actions write useful_new_count to changelog
    When we store "$1, $2" into "inbox"
    And we store "$3" into "trash"
    And we <unsubscribed_action>
    Then "<change_type>" is last changelog entry with "2" as useful_new_count

    Examples:
      | unsubscribed_action                 | change_type              |
      | remove "{Outbox}" from unsubscribed | imap-delete-unsubscribed |
      | add "{Foo}" to unsubscribed         | imap-add-unsubscribed    |

  @other-user
  Scenario Outline: Imap unsubscribe actions do not touch other users
    Given replication stream
    When we <unsubscribed_action>
    Then there are only our user changes in replication stream

    Examples:
      | unsubscribed_action                 |
      | remove "{Outbox}" from unsubscribed |
      | add "{Foo}" to unsubscribed         |
