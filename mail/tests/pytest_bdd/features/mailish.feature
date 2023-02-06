Feature: Xeno mail

  Background: All on inited mailish user
    Given inited mailish user with fid 1

  Scenario: save account
    When we initialize new user "Mail"
    And we save account "Mail"
      """
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '123'
          auth_type: password
          uuid: 12f436
          oauth_app: mailru-o1
          last_sync: !!timestamp "1996-07-17 02:44:25 -7"
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: ab538f66b
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: ab538f66b
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '124'
          auth_type: oauth2
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-17 02:44:25 -7"
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '125'
          auth_type: password
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-18 02:44:25 -7"
      """
    And invalidate mailish token 125
    Then we have 2 auth data
    And we have 2 security locks
    And token "124" has data
      """
      oauth_app: 'mailru-o2'
      """
    And account has data
      """
      last_sync: !!timestamp "1996-07-17 02:44:25 -7"
      """

  Scenario: erase security locks
    When we initialize new user "Mail"
    And we save account "Mail"
      """
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '125'
          auth_type: password
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-18 02:44:25 -7"
      """
    And we erase mailish security locks
    Then we have 0 security locks

  Scenario: init mailish folder
    When we initialize new user "Mail"
    When we save account "Mail"
      """
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '125'
          auth_type: password
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-18 02:44:25 -7"
      """
    And we init existing folder as mailish
      | fid | path  | uidvalidity |
      | 1   | INBOX | 109291      |
    Then we have mailish data for fid 1
      """
      imap_path: 'INBOX'
      uidvalidity: 109291
      """
    And we have downloaded range for fid 1 equals (0, 0)

  Scenario: empty imap path error
    When we initialize new user "Mail"
    When we save account "Mail"
      """
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '125'
          auth_type: password
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-18 02:44:25 -7"
      """
    And we try init existing folder as mailish with op_id "$try-create-mailish-folder"
      | fid | path | uidvalidity |
      | 1   |      | 109291      |
    Then commit "$try-create-mailish-folder" should produce "MailishFolderWithEmptyImapPath"

  Scenario: check init mailish folder restrictions
    When we initialize new user "Mail"
    When we save account "Mail"
      """
      -   email: test@mail.ru
          imap_login: test@mail.ru
          imap_credentials: qwerty
          imap_server: imap.mail.ru
          imap_port: 993
          imap_ssl: True
          smtp_login: test@mail.ru
          smtp_credentials: qwerty
          smtp_server: smtp.mail.ru
          smtp_port: 465
          smtp_ssl: True
          token_id: '125'
          auth_type: password
          uuid: 13a4c6
          oauth_app: mailru-o2
          last_sync: !!timestamp "1996-07-18 02:44:25 -7"
      """
    And we try store "$1" into "inbox" as "$store"
    And we try init existing folder as mailish with op_id "$try-create-mailish-folder"
      | fid | path  | uidvalidity |
      | 1   | INBOX | 109291      |
    And we commit "$store"
    Then commit "$try-create-mailish-folder" should produce "CreatingMailishFolderFromNonEmptyFolder"

  Scenario: update downloaded range
    When we updating downloaded range
      | fid | range_start | range_end |
      | 1   | 10          | 194       |
    Then we have downloaded range for fid 1 equals (10, 194)

  Scenario: update downloaded range with invalid values
    When we try updating downloaded range as "$try-update-downloaded-range"
      | fid | range_start | range_end |
      | 1   | 200         | 194       |
    Then commit "$try-update-downloaded-range" should produce "UpdatingMailishDownloadedRangeWithInvalidValues"

  Scenario: update mailish folder data
    When we updating mailish folder
      | fid | path    | uidvalidity |
      | 1   | INBOX_1 | 109291      |
    Then we have mailish data for fid 1
      """
      imap_path: 'INBOX_1'
      uidvalidity: 109291
      """

  Scenario: delete mailish folder entries
    When we delete mailish folder entries
      | fid |
      | 1   |
    Then we have 0 mailish folders

  Scenario: check delete folder restrictions
    When we store "$1" into "inbox"
      | mailish                       |
      | 932%1943-10-11 19:00:00+03:00 |
    And we try delete mailish folder entry as "$try-delete-mailish-folder"
      | fid |
      | 1   |
    Then commit "$try-delete-mailish-folder" should produce "DeletingNonEmptyMailishFolderEntry"

  Scenario: check add_mailish_entries and delete_mailish_folder working concurrenly
    When we try delete mailish folder entry as "$try-delete-mailish-folder"
      | fid |
      | 1   |
    And we try to add mailish entries as "$try-add-mailish-entry"
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
    And we commit "$try-delete-mailish-folder"
    Then commit "$try-add-mailish-entry" should produce "AddingMailishEntryToNonMailishFolder"

  Scenario: try init mailish folder without account
    When we initialize new user "Mail"
    When we try init existing folder as mailish with op_id "$try-create-mailish-folder"
      | fid | path  | uidvalidity |
      | 1   | INBOX | 109291      |
    Then commit "$try-create-mailish-folder" should produce "CreatingMailishFolderWithoutMailishAccount"

  Scenario: add mailish entries
    When we add mailish entries
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 91      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 92      |
    Then we have 3 items in mailish folder 1

  Scenario: check mailish restrictions
    When we initialize new user "Mail"
    When we try to add mailish entries as "$try-add-mailish"
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
    Then commit "$try-add-mailish" should produce "AddingMailishEntryToNonMailishFolder"

  Scenario: delete mailish entry
    When we add mailish entries
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 91      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 92      |
    And we deleting mailish entry
      | fid | imap_id |
      | 1   | 90      |
    Then we have 2 items in mailish folder 1

  Scenario: check delete restrictions
    When we try store "$1" into "inbox" as "$store"
      | mailish                       |
      | 932%1943-10-11 19:00:00+03:00 |
    And we try delete mailish entry as "$try-delete-mailish"
      | fid | imap_id |
      | 1   | 932     |
    And we commit "$store"
    Then commit "$try-delete-mailish" should produce "DeletingMailishEntryWithNonEmptyMid"

  Scenario: add mailish entry and increment error
    When we add mailish entries
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 91      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 92      |
    And we increment mailish entry errors count
      | fid | imap_id |
      | 1   | 90      |
    Then we have errors count equals 1 for entry
      | fid | imap_id |
      | 1   | 90      |

  Scenario: add mailish entry and increment error
    When we init existing folder as mailish
      | fid | path      | uidvalidity |
      | 2   | NOT_INBOX | 109291      |
    And we add mailish entries
      | fid | imap_time                  | imap_id |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 90      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 91      |
      | 1   | 17-Jul-1996 02:44:25 -0700 | 92      |
    And we move mailish messages to fid 2 from fid 1
      | from_id | to_id |
      | 90      | 50    |
    Then we have 2 items in mailish folder 1
    And we have 1 items in mailish folder 2

  Scenario: increment mailish entry errors count
    When we increment mailish entry errors count
      | fid | imap_id | errors |
      | 1   | 90      | 1      |
      | 1   | 91      | 2      |
      | 1   | 92      | 3      |
    Then we have errors count equals 3 for entry
      | fid | imap_id |
      | 1   | 92      |
    And we have 3 items in mailish folder 1

  Scenario: update mailish message errors count
    When we increment mailish entry errors count
      | fid | imap_id | errors |
      | 1   | 90      | 1      |
    And we increment mailish entry errors count
      | fid | imap_id | errors |
      | 1   | 90      | 4      |
    Then we have errors count equals 5 for entry
      | fid | imap_id |
      | 1   | 90      |
    And we have 1 items in mailish folder 1

  Scenario: check mailish restrictions
    When we initialize new user "Mail"
    When we try to increment mailish entry errors count as "$try-save-mailish"
      | fid | imap_id | errors |
      | 1   | 90      | 1      |
    Then commit "$try-save-mailish" should produce "AddingMailishEntryToNonMailishFolder"

  Scenario: accounts last sync updated
    When we update mailish account last sync with new date "17-Jul-2017 02:44:25 -0700"
    Then account has data
      """
     last_sync: !!timestamp "2017-07-17 02:44:25 -7"
      """
