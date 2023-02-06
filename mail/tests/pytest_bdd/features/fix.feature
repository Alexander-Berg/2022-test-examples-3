Feature: Test for util.fix_* functions

  Scenario: Fix do nothing on 'good' users
    Given new initialized user
    When we fix "all"
    Then fix log is empty
    When we store "$[1:3]" into tab "relevant"
    And we fix "all"
    Then fix log is empty

  Scenario: [PS-1544] Remove labels from mail.labels dictionary
    Given new initialized user
    When we create "user" label "victim"
    And we store into "inbox"
      | mid | label                     |
      | $1  | user:victim               |
      | $2  | user:victim, system:draft |
      | $3  |                           |
    Then "user" label "victim" has "2" messages
    When we accidentally delete "user" label "victim"
    Then check produce
      | name            |
      | box.missed_lids |
    When we fix "labels"
    Then check produce nothing
    And fix log contains
      | name            |
      | box.missed_lids |

  Scenario: [MPROTO-2185] Remove system label from mail.labels dictionary
    Given new initialized user
    When we accidentally delete "system" label "pinned"
    Then "system" label "pinned" does not exist
    And check produce
      | name                 |
      | labels.missed_system |
    When we fix "all"
    Then check produce nothing
    And fix log contains
      | name                 |
      | labels.missed_system |

  Scenario: Create new system folder when it `disappear`
    Given new initialized user
    When we accidentally delete "inbox" folder
    Then "inbox" folder does not exist
    And check produce
      | name                  |
      | folders.missed_system |
    When we fix "all"
    Then check produce nothing
    And fix log contains
      | name                  |
      | folders.missed_system |
    And "inbox" has fid "7" and unique type

  Scenario: Restore folder type, if exists with `system` name
    Given new initialized user
    When we accidentally set "inbox" type to "user"
    Then "inbox" folder does not exist
    And folder named "Inbox" has fid "1"
    And check produce
      | name                  |
      | folders.missed_system |
    When we fix "defaults"
    Then check produce nothing
    And "inbox" has fid "1"
    And fix log contains
      | name                  |
      | folders.missed_system |

  Scenario: Folder with restored type should have unique type
    Given new initialized user
    Then "inbox" has fid "1"
    When we accidentally set "inbox" counters
      | column      | invalid_value |
      | type        | user          |
      | unique_type | false         |
    Then folder named "Inbox" has not unique type
    Then check produce
      | name                  |
      | folders.missed_system |
    When we fix "defaults"
    Then check produce nothing
    And "inbox" has fid "1" and unique type

  Scenario: Restore defaults, restore type only on root folders
    Given new initialized user
    When we create "user" folder "Inbox" under "Trash"
    Then folder named "Trash|Inbox" has fid "7"
    When we accidentally delete "inbox" folder
    Then "inbox" folder does not exist
    And check produce
      | name                  |
      | folders.missed_system |
    When we fix "defaults"
    Then check produce nothing
    And fix log contains
      | name                  |
      | folders.missed_system |
    And "inbox" has fid "8"
    And serial "next_fid" is "9"

  Scenario: Unsupported case, Inbox folder has discount type
    Given new initialized user
    When we accidentally set "inbox" type to "discount"
    Then check produce
      | name                  |
      | folders.missed_system |
    When we try fix "all" as "$unsupported-fix"
    Then commit "$unsupported-fix" should produce "UnexpectedBehaviorError"

  Scenario: [MAILPG-479] Break serial.next_mid_serial
    Given new initialized user
    When we store "$[1:3]" into "inbox"
    Then serial "next_mid_serial" is "4"
    When we accidentally set serial "next_mid_serial" to "1"
    Then check produce
      | name                                               |
      | mid2serial(mail.box.mid) vs serial.next_mid_serial |
    When we fix "serials"
    Then check produce nothing
    Then serial "next_mid_serial" is "4"
    And fix log contains
      | name                    |
      | serials.next_mid_serial |

  Scenario: Check fix next_mid_serial for deleted messages
    Given new initialized user
    When we store "$[1:3]" into "inbox"
    And we delete "$3"
    Then serial "next_mid_serial" is "4"
    When we accidentally set serial "next_mid_serial" to "1"
    Then check produce
      | name                                               |
      | mid2serial(mail.box.mid) vs serial.next_mid_serial |
    When we fix "serials"
    Then check produce nothing
    Then serial "next_mid_serial" is "4"
    And fix log contains
      | name                    |
      | serials.next_mid_serial |

  Scenario: Break serials and delete system label. Fix only defaults
    Given new initialized user
    When we accidentally delete "system" label "pinned"
    And we accidentally set serial "next_lid" to "1"
    Then "system" label "pinned" does not exist
    And check produce
      | name                                                        |
      | labels.missed_system                                        |
      | labels.{lid, revision} vs serials.{next_fid, next_revision} |
    When we fix "defaults"
    Then check produce nothing
    And fix log contains
      | name                 |
      | serials.next_lid     |
      | labels.missed_system |

  Scenario: Break serial.next_lid
    Given new initialized user
    When we create "user" label "apple"
    And we accidentally set serial "next_lid" to "1"
    Then check produce
      | name                                                        |
      | labels.{lid, revision} vs serials.{next_fid, next_revision} |
    When we try create "user" label "banana" as "$label-create"
    Then commit "$label-create" should produce "LabelWithSameLidAlreadyExists"
    When we fix "serials"
    Then check produce nothing
    When we create "user" label "banana"
    Then "user" label "banana" exists
    And fix log contains
      | name             |
      | serials.next_lid |

  Scenario: Break serials.next_fid
    Given new initialized user
    When we accidentally set serial "next_fid" to "1"
    Then check produce
      | name                                                         |
      | folders.{fid, revision} vs serials.{next_fid, next_revision} |
    When we try create "user" folder "foo" as "$folder-create"
    Then commit "$folder-create" should produce "FolderWithSameFidAlreadyExists"
    When we fix "serials"
    Then check produce nothing
    When we create "user" folder "foo"
    Then folder named "foo" has no messages
    And fix log contains
      | name             |
      | serials.next_fid |

  Scenario: Break serials.next_revision after store
    Given new initialized user
    When we store "$1" into "inbox"
    And we accidentally set serial "next_revision" to "1"
    Then check produce
      | name                                                         |
      | folders.{fid, revision} vs serials.{next_fid, next_revision} |
      | labels.{lid, revision} vs serials.{next_fid, next_revision}  |
      | tabs.{revision} vs serials.{next_revision}                   |
      | counters.{revision} vs serials.{next_revision}               |
    When we fix "serials"
    Then check produce nothing
    And fix log contains
      | name                  |
      | serials.next_revision |

  Scenario: Break serials.next_owner_subscription_id
    Given shared folder with subscription
    When we accidentally set serial "next_owner_subscription_id" to "1"
    Then check produce
      | name                               |
      | serials.next_owner_subscription_id |
    When we fix "serials"
    Then check produce nothing
    And fix log contains
      | name                               |
      | serials.next_owner_subscription_id |

  Scenario: Break serials.next_subscriber_subscription_id
    When we initialize new user "Postgre" with "inbox" shared folder
    Given new initialized user with "user" folder "hackers"
    When we add "inbox@Postgre" as "hackers" to subscribed folders
    And we accidentally set serial "next_subscriber_subscription_id" to "1"
    Then check produce
      | name                                    |
      | serials.next_subscriber_subscription_id |
    When we fix "serials"
    Then check produce nothing
    And fix log contains
      | name                                    |
      | serials.next_subscriber_subscription_id |

  Scenario Outline: Break mail.box.chain
    Given new initialized user
    When we store "$[1:4]" into "inbox"
    Then chained "inbox" is
      | 1 2 3 | 4 |
    When we accidentally set chain to "NULL" on "$1"
    And we accidentally set chain to "4" on "$4"
    Then check produce
      | name                    |
      | box.chain               |
      | box.chain.first_imap_id |
    When we fix "<fix_target>"
    Then check produce nothing

    Examples:
      | fix_target |
      | chains     |
      | all        |

  Scenario: Recreate imap chains
    Given new initialized user
    When we store "$[1:6]" into "inbox"
    Then chained "inbox" is
      | 1 2 3 | 4 5 6 |
    When we recreate chains for "inbox" with step "2"
    Then chained "inbox" is
      | 1 2 | 3 4 | 5 6 |

  Scenario Outline: Break folders.<counter>
    Given new initialized user
    When we store into "inbox"
      | mid | size |
      | $1  | 10   |
      | $2  | 20   |
    Then "inbox" has "0" seen, "2" recent, "30" size
    When we accidentally set "inbox" <counter> to "<invalid_value>"
    Then check produce
      | name    |
      | folders |
    When we fix "folders"
    Then check produce nothing
    And fix log contains
      | name                   |
      | folders.fixed_counters |

    Examples:
      | counter        | invalid_value |
      | message_size   | 42            |
      | message_seen   | 1             |
      | message_recent | 1             |

  Scenario Outline: Break folders.<pointer>
    Given new initialized user
    When we store "$[1:3]" into "inbox"
    Then "inbox" has first_unseen at "1"
    When we accidentally set "inbox" <pointer> to "<invalid_value>"
    Then check produce
      | name                 |
      | folders.first_unseen |
    When we fix "folders"
    Then check produce nothing
    And fix log contains
      | name                   |
      | folders.fixed_counters |

    Examples:
      | pointer         | invalid_value |
      | first_unseen    | 2             |
      | first_unseen_id | 2             |

    # We can't simple break message_count,
    # cause in mail.folders has constraint
    #    message_count < next_imap_id
  Scenario: Break folders.message_count
    Given new initialized user
    When we store "$[1:3]" into "inbox"
    And we move "$[1:2]" to "trash"
    Then "inbox" has "1" message
    When we accidentally set "inbox" message_count to "3"
    Then check produce
      | name    |
      | folders |
    When we fix "folders"
    Then fix log contains
      | name                   |
      | folders.fixed_counters |

    # checks and fixes should
    # deal with NULL-s
    # from mail.box aggregates
    # in folders without messages
  Scenario: Break empty folder
    Given new initialized user
    When we accidentally set "inbox" counters
      | column        | invalid_value |
      | message_count | 3             |
      | message_size  | 30            |
      | next_imap_id  | 4             |
    Then check produce
      | name    |
      | folders |
    When we fix "folders"
    Then fix log contains
      | name                   |
      | folders.fixed_counters |
    And check produce nothing

  Scenario Outline: Break label message_count
    Given new initialized user
    When we create "user" label "victim"
    And we store "$1, $2" into "inbox"
      | label       |
      | user:victim |
    Then "user" label "victim" has "2" messages
    When we accidentally set message_count to "<invalid_count>" for "user" label "victim"
    Then check produce
      | name   |
      | labels |
    When we fix "labels"
    Then fix log contains
      | name                  |
      | labels.fixed_counters |
    And check produce nothing

    Examples:
      | invalid_count |
      | 0             |
      | 1             |
      | 42            |

  Scenario: Break label message_count for empty label
    Given new initialized user
    When we create "user" label "victim"
    And we accidentally set message_count to "42" for "user" label "victim"
    Then check produce
      | name   |
      | labels |
    When we fix "labels"
    Then fix log contains
      | name                  |
      | labels.fixed_counters |
    And check produce nothing

  Scenario: Break all together
    Given new initialized user
    When we create "user" label "victim"
    And we create "user" label "injured"
    And we store "$[1:3]" into "inbox"
    And we set "+user:victim" on "$3"
    Then global revision is "7"
    When we accidentally set serial "next_mid_serial" to "1"
    And we accidentally set serial "next_lid" to "1"
    And we accidentally set serial "next_fid" to "1"
    And we accidentally set serial "next_revision" to "1"
    And we accidentally delete "user" label "victim"
    And we accidentally set message_count to "13" for "user" label "injured"
    And we accidentally set "inbox" counters
      | column         | invalid_value |
      | message_count  | 5             |
      | next_imap_id   | 7             |
      | message_seen   | 4             |
      | message_recent | 0             |
    Then check produce
      | name                                                         |
      | mid2serial(mail.box.mid) vs serial.next_mid_serial           |
      | folders.{fid, revision} vs serials.{next_fid, next_revision} |
      | folders                                                      |
      | labels                                                       |
      | labels.{lid, revision} vs serials.{next_fid, next_revision}  |
      | box.missed_lids                                              |
      | tabs.{revision} vs serials.{next_revision}                   |
      | counters.{revision} vs serials.{next_revision}               |
    When we setup replication stream
    When we fix "all"
    Then check produce nothing
    And global revision is "8"
    And fix log contains
      | name                    | revision |
      | serials.next_lid        | 8        |
      | serials.next_fid        | 8        |
      | serials.next_revision   | 8        |
      | serials.next_mid_serial | 8        |
      | box.missed_lids         | 8        |
    And there are only our user changes in replication stream

  Scenario Outline: [MOBILEMAIL-6183] fix non positive lids
    Given new initialized user
    When we accidentally set lid for "system" label "pinned" to "<lid>"
    Then "system" label "pinned" has lid "<lid>"
    When we store "$1" into "inbox"
      | labels        |
      | system:pinned |
    And we fix not positive lids
    Then "system" label "pinned" has positive lid
    And in "inbox" there is one message
      | mid | labels        |
      | $1  | system:pinned |

    Examples:
      | lid |
      | 0   |
      | -42 |

  Scenario: [MAILWEB-1085] Remove sms from messages.attributes
    Given new initialized user
    When we store into "inbox"
      | mid | attributes |
      | $1  | sms, spam  |
      | $2  | sms        |
      | $3  | postmaster |
    And we execute "remove_sms_attribute" util
    Then in "inbox" there are "3" messages
      | mid | attributes |
      | $1  | spam       |
      | $2  |            |
      | $3  | postmaster |

  @[MAILPG-796] @doomed
  Scenario: Fix doomed for doomed folder
  Checks and fixes should find and fix
  messages without doom_date
  in doomed folders
    Given new initialized user
    When we store "$1" into "trash"
    Then message "$1" has recent doom_date
    When we accidentally set doom_date for "$1" to "NULL"
    Then check produce
      | name                                       |
      | message_in_doomed_folder_without_doom_date |
    When we fix "doomed"
    Then check produce nothing
    And fix log contains
      | name       |
      | fix_doomed |

  @[MAILPG-796] @doomed
  Scenario: Fix doomed for not doomed folder
  Checks and fixes should find and fix
  messages with doom_date
  in not doomed folders
    Given new initialized user
    When we store "$1" into "inbox"
    Then message "$1" has null doom_date
    When we accidentally set doom_date for "$1" to "now"
    Then check produce
      | name                                        |
      | message_in_not_doomed_folder_with_doom_date |
    When we fix "doomed"
    Then check produce nothing
    And fix log contains
      | name       |
      | fix_doomed |

  @[MAILPG-796] @doomed
  Scenario: Fix all for both doomed cases
    Given new initialized user
    When we store "$blessed" into "inbox"
    And we store "$doomed" into "trash"
    And we accidentally set doom_date for "$blessed" to "now"
    And we accidentally set doom_date for "$doomed" to "NULL"
    Then check produce
      | name                                        |
      | message_in_doomed_folder_without_doom_date  |
      | message_in_not_doomed_folder_with_doom_date |
    When we fix "all"
    Then check produce nothing
    And fix log contains
      | name       |
      | fix_doomed |

  @[MAILPG-796]  @threaded
  Scenario: Message without tid in threaded folder
    Given new initialized user
    When we store into "inbox"
      | mid | tid |
      | $1  | 100 |
    Then in "inbox" there is one thread
      | mid | tid |
      | $1  | 100 |
    When message "$1" accidentally lose thread
    Then check produce
      | name                                   |
      | message_in_threaded_folder_without_tid |
    When we fix "threaded"
    Then check produce nothing
    And in "inbox" there is one thread
      | mid | tid |
      | $1  | 100 |

  @[MAILPG-796]  @threaded
  Scenario: Message with tid in unthreaded folder
    Given new initialized user
    When we store into "trash"
      | mid | tid |
      | $1  | 100 |
    Then in "trash" there are no threads
    When message "$1" accidentally gain thread
    Then in "trash" there is one thread
      | mid | tid |
      | $1  | 100 |
    Then check produce
      | name                                    |
      | message_in_not_threaded_folder_with_tid |
    When we fix "threaded"
    Then check produce nothing
    And in "trash" there are no threads

  @[MAILPG-796]  @threaded
  Scenario: Fix all should handle both threaded cases
    Given new initialized user
    When we store into "inbox"
      | mid        | tid |
      | $inbox-mid | 100 |
    And we store into "trash"
      | mid        | tid |
      | $trash-mid | 200 |
    And message "$inbox-mid" accidentally lose thread
    And message "$trash-mid" accidentally gain thread
    Then check produce
      | name                                    |
      | message_in_threaded_folder_without_tid  |
      | message_in_not_threaded_folder_with_tid |
    When we fix "all"
    Then check produce nothing

  @DARIA-57183
  Scenario: Fix_so_label_types
  replace `domain` labels with digit
  names with `type` labels
    Given new initialized user
    When we create "domain" label "42"
    And we store "$1" into "inbox"
      | label     |
      | domain:42 |
    Then in "inbox" there is one message
      | mid | labels    |
      | $1  | domain:42 |
    When we fix_so_label_types
    Then in "inbox" there is one message
      | mid | labels  |
      | $1  | type:42 |
    And "domain" label "42" does not exist

  @DARIA-57183
  Scenario: Fix_so_label_type when exist same type label
    Given new initialized user
    When we create "domain" label "13"
    And we create "type" label "13"
    And we store into "inbox"
      | mid | labels             |
      | $1  | domain:13          |
      | $2  | type:13            |
      | $3  | domain:13, type:13 |
    And we fix_so_label_types
    Then in "inbox" there are "3" messages
      | mid | labels  |
      | $1  | type:13 |
      | $2  | type:13 |
      | $3  | type:13 |

  @DARIA-57183
  Scenario: Fix_so_label_type change lids in trash
    Given new initialized user
    When we create "domain" label "13"
    And we store into "trash"
      | mid | labels    |
      | $1  | domain:13 |
    And we fix_so_label_types
    Then in "trash" there is one message
      | mid | labels  |
      | $1  | type:13 |
    And "domain" label "13" does not exist

  @DARIA-57183
  Scenario: Fix_so_label_type do not touch over domain labels
    Given new initialized user
    When we create "domain" label "yaru"
    And we create "domain" label "17"
    And we store into "inbox"
      | mid | label                  |
      | $1  | domain:yaru            |
      | $2  | domain:17              |
      | $3  | domain:yaru, domain:17 |
    And we fix_so_label_types
    Then in "inbox" there are "3" messages
      | mid | label                |
      | $1  | domain:yaru          |
      | $2  | type:17              |
      | $3  | domain:yaru, type:17 |

  @DARIA-57183
  Scenario: Fix_so_label_type do not touch user without problem labels
    Given new initialized user
    When we create "domain" label "st"
    And we create "type" label "10"
    And we store into "inbox"
      | mid | label     |
      | $1  | domain:st |
      | $2  | type:10   |
      | $3  |           |
    Then global revision is "6"
    When we fix_so_label_types
    Then global revision is "6"

  @DARIA-57183
  Scenario: Fix_so_label_type should change type only
  for domain labels with digit names
    Given new initialized user
    When we create "domain" label "vtb24"
    And we create "domain" label "2k"
    And we store into "inbox"
      | mid | label        |
      | $1  | domain:vtb24 |
      | $2  | domain:2k    |
    Then global revision is "5"
    When we fix_so_label_types
    Then global revision is "5"

  @DARIA-57183 @MAILPG-954
  Scenario: Fix_so_label_type should split changes onto parts
    Given new initialized user
    When we create "domain" label "42"
    And we store "$[1:5]" into "inbox"
      | label     |
      | domain:42 |
    Then global revision is "7"
    When we fix_so_label_types
  # 1 label_create +  2 * message_update + 1 delete_label
    Then global revision is "11"
    And in changelog there are
      | revision | type         |
      | 8        | label-create |
      | 9        | update       |
      | 10       | update       |
      | 11       | label-delete |

  @MAILDEV-447
  Scenario: Remove angle brackets from recipients emails
    Given new initialized user
    When we store into "inbox"
      | mid | recipients                                                                               |
      | $1  | from:Vasya:<vasya@yandex.ru>, to:Sanya:sanya@yandex.ru, reply-to:Vasya:<vasya@yandex.ru> |
      | $2  | from:Vanya:vanya@yandex.ru, to:Kuzya:kuzya@yandex.ru                                     |
      | $3  | from:Valya:<valya@yandex.ru>, to:Tanya:<tanya@yandex.ru>                                 |
    And we execute "remove_recipient_brackets" util
    Then in "inbox" there are "3" messages
      | mid | recipients                                                                           |
      | $1  | from:Vasya:vasya@yandex.ru, to:Sanya:sanya@yandex.ru, reply-to:Vasya:vasya@yandex.ru |
      | $2  | from:Vanya:vanya@yandex.ru, to:Kuzya:kuzya@yandex.ru                                 |
      | $3  | from:Valya:valya@yandex.ru, to:Tanya:tanya@yandex.ru                                 |

  Scenario Outline: Fix broken tabs.<counter>
    Given new initialized user
    When we store into tab "relevant"
      | mid | size |
      | $1  | 10   |
      | $2  | 20   |
    Then tab "relevant" has "2" messages, "2" unseen, "30" size
    When we accidentally set tab "relevant" <counter> to "<invalid_value>"
    Then check produce
      | name |
      | tabs |
    When we fix "tabs"
    Then check produce nothing
    And fix log contains
      | name                |
      | tabs.fixed_counters |
    Examples:
      | counter       | invalid_value |
      | message_count | 1             |
      | message_seen  | 1             |
      | message_size  | 42            |

  Scenario: Fix broken empty tabs
    Given new initialized user
    When we accidentally set tab "news" counters
      | column        | invalid_value |
      | message_count | 3             |
      | message_seen  | 2             |
      | message_size  | 30            |
    Then check produce
      | name |
      | tabs |
    When we fix "tabs"
    Then fix log contains
      | name                |
      | tabs.fixed_counters |
    And check produce nothing

  Scenario Outline: Break threads newest
    Given new initialized user
    When we store into tab "relevant"
      | mid | tid |
      | $1  | 42  |
      | $2  | 42  |
    Then in "inbox" there is one thread
      | tid | mid |
      | 42  | $2  |
    And in tab "relevant" there is one thread
      | tid | mid |
      | 42  | $2  |
    When we accidentally break thread "42"
      | mid | <broken> |
      | $1  | <first>  |
      | $2  | <second> |
    Then check produce
      | name         |
      | box.<broken> |
    When we fix "threads_newest_<fix>"
    Then check produce nothing
    And in "inbox" there is one thread
      | tid | mid |
      | 42  | $2  |
    And in tab "relevant" there is one thread
      | tid | mid |
      | 42  | $2  |
    And fix log contains
      | name                       |
      | box.<broken>.fixed_threads |
    Examples:
      | broken     | fix        | first | second |
      | newest_tif | in_folders | false | false  |
      | newest_tif | in_folders | true  | false  |
      | newest_tif | in_folders | true  | true   |
      | newest_tit | in_tabs    | false | false  |
      | newest_tit | in_tabs    | true  | false  |
      | newest_tit | in_tabs    | true  | true   |

  Scenario Outline: Fix broken attach counters
    Given new initialized user
    When we store into tab "relevant"
      | mid | flags | attaches                   |
      | $1  | seen  | 1.2:image/jpg:apple.jpg:0  |
      | $2  |       | 1.2:image/jpg:banana.jpg:0 |
    When we accidentally set attach counter <counter> to "<invalid_value>"
    Then check produce
      | name |
      | counters |
    When we fix "counters"
    Then check produce nothing
    And fix log contains
      | name                    |
      | counters.fixed_attaches |
    And user has "2" messages with attaches, one seen
    Examples:
      | counter            | invalid_value |
      | has_attaches_count | 3             |
      | has_attaches_seen  | 2             |

  Scenario: Fix broken empty attach counters
    Given new initialized user
    When we accidentally set attach counters
      | column             | invalid_value |
      | has_attaches_count | 3             |
      | has_attaches_seen  | 1             |
    Then check produce
      | name |
      | counters |
    When we fix "counters"
    Then check produce nothing
    And fix log contains
      | name                    |
      | counters.fixed_attaches |
    And user has no messages with attaches

  Scenario: Fix broken label's message_seen
    Given new initialized user
    When we create "user" label "apple"
    And we store into "inbox" 
      | mid | label      |
      | $1  | user:apple |
      | $2  | user:apple |
    Then "inbox" has "2" messages, "2" unseen
    And "user" label "apple" has "2" messages
    When we accidentally set message_seen to "1" for "user" label "apple"
    Then "user" label "apple" has "1" seen messages
    And "user" label "apple" has "2" messages
    And "inbox" has "2" messages, "2" unseen
    Then check produce
      | name   |
      | labels |
    When we fix "labels"
    Then check produce nothing
    And "user" label "apple" has "0" seen messages
    And "user" label "apple" has "2" messages
    And "inbox" has "2" messages, "2" unseen
