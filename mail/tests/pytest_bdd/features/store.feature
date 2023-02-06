Feature: Store messages

  Background: All on new user
    Given new initialized user

  Scenario: Store message 3 messages
    When we store "$1" into "inbox"
      | flags | size |
      |       | 10   |
    Then "inbox" has one message, "1" unseen, "1" recent, "10" size at revision "2"
    When we store "$2" into "inbox"
      | flags | size |
      | seen  | 15   |
    Then "inbox" has "2" messages, "1" unseen, "2" recent, "25" size at revision "3"
    When we store "$3" into "inbox"
      | flags | size |
      | seen  | 25   |
    Then "inbox" has "3" messages, "1" unseen, "3" recent, "50" size at revision "4"
    And in "inbox" there are "3" messages
      | mid | flags        | revision |
      | $1  | recent       | 2        |
      | $2  | recent, seen | 3        |
      | $3  | recent, seen | 4        |
    And in "inbox" there are "3" threads
      | tid | count | unseen |
      | $1  | 1     | 1      |
      | $2  | 1     | 0      |
      | $3  | 1     | 0      |

  Scenario: Store 3 messages in one thread
    When we store into "inbox"
      | mid | tid | flags |
      | $1  | 1   |       |
      | $2  | 1   | seen  |
      | $3  | 1   |       |
    Then "inbox" has "3" messages, "2" unseen, "3" recent at revision "4"
    And in "inbox" there are "3" messages
      | mid | tid | flags        | revision |
      | $1  | 1   | recent       | 2        |
      | $2  | 1   | recent, seen | 3        |
      | $3  | 1   | recent       | 4        |
    And in "inbox" there is one thread
      | tid | count | unseen |
      | 1   | 3     | 2      |

  Scenario: Store into same thread should increase revision
    When we store into "inbox"
      | mid | tid |
      | $1  | 1   |
    Then "inbox" has one message at revision "2"
    And in "inbox" there is one thread
      | mid | tid | revision |
      | $1  | 1   | 2        |
    When we store into "sent"
      | mid | tid |
      | $2  | 1   |
    Then "sent" has one message at revision "3"
    And in "sent" there is one thread
      | mid | tid | thread_revision | count |
      | $2  | 1   | 3               | 2     |
    And in "inbox" there is one thread
      | mid | tid | thread_revision | count |
      | $1  | 1   | 3               | 2     |

  Scenario Outline: Store message with references and in_reply_to
    When we store "$1" into "inbox"
      | references   | in_reply_to   |
      | <references> | <in_reply_to> |
    Then message "$1" has "<count>" references
    Examples:
      | references | in_reply_to | count |
      | r1, r2     |             | 2     |
      | r1, r2     | r3          | 3     |
      | r1, r2     | r1          | 2     |
      | r1, r2     | r2          | 2     |

  Scenario: Store into Spam
  Messages stored in Spam or Trash
  should not have tid
    When we store "$1" into "inbox"
      | tid |
      | 1   |
    And we store "$2" into "spam"
      | tid |
      | 1   |
    Then in "inbox" there is one message
    And in "spam" there is one message
    And in "inbox" there is one thread
      | tid | count |
      | 1   | 1     |
    And message "$1" has threading info with tid "1"

  Scenario: Store into Spam should set doom_date
    When we store "$1" into "spam"
    Then message "$1" has recent doom_date

  Scenario: Store messages with labels
    When we create "user" label "funny"
    And we store "$1" into "inbox"
      | tid | label      |
      | 13  | user:funny |
    Then in "inbox" there is one message
      | mid | tid | label      |
      | $1  | 13  | user:funny |
    And "user" label "funny" has one message
    And in "inbox" there is one thread
      | tid | count | thread_labels |
      | 13  | 1     | user:funny=1  |
    When we store into "inbox"
      | mid | tid | labels                   |
      | $2  | 13  | user:funny, system:draft |
      | $3  | 13  |                          |
      | $4  | 13  | system:draft             |
    Then in "inbox" there is one thread
      | tid | count | thread_labels                |
      | 13  | 4     | user:funny=2, system:draft=2 |
    When we store into "drafts"
      | mid | tid | label        |
      | $5  | 13  | user:funny   |
      | $6  | 13  | system:draft |
    Then in folders "inbox, drafts" there is one thread
      | tid | count | thread_labels                |
      | 13  | 6     | user:funny=3, system:draft=3 |

  Scenario: Check imap_id and chain
    When we store "$1" into "inbox"
    Then in IMAP "inbox" there is one message
      | mid | imap_id | revision |
      | $1  | 1       | 2        |
    And chained "inbox" is
      | 1 |
    And "inbox" has one message, "1" unseen, one recent at revision "2"
    When we store "$2" into "inbox"
    Then in IMAP "inbox" there are "2" messages
      | mid | imap_id | revision |
      | $1  | 1       | 2        |
      | $2  | 2       | 3        |
    And chained "inbox" is
      | 1 2 |

  Scenario: Create chain for IMAP
    When we store "$[1:3]" into "inbox"
    Then in IMAP "inbox" there are "3" messages
    And chained "inbox" is
      | 1 2 3 |
    When we store "$4" into "inbox"
    Then in IMAP "inbox" there are "4" messages
    And chained "inbox" is
      | 1 2 3 | 4 |

  Scenario: Store 7 messages check chains
    When we store "$[1:7]" into "inbox"
    Then "inbox" has "7" messages
    And chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: IMAP first_unseen with first as unseen
    Then "inbox" has first_unseen at "0"
    When we store "$1" into "inbox"
    Then "inbox" has first_unseen at "1"
    When we store "$2" into "inbox"
    Then "inbox" has first_unseen at "1"

  Scenario: IMAP first_unseen with second unseen
    When we store into "inbox"
      | mid | flags |
      | $10 | seen  |
    Then "inbox" has first_unseen at "0"
    When we store "$11" into "inbox"
    Then "inbox" has first_unseen at "2"

  Scenario Outline: No fresh increment for special folders
    When we store "$1" into "<folder_type>"
    Then fresh counter is "0" and has revision "any"
    Examples:
      | folder_type |
      | trash       |
      | spam        |
      | drafts      |
      | sent        |

  Scenario: Store seen message into inbox
    When we store "$1" into "inbox"
      | flags |
      | seen  |
    Then fresh counter is "0" and has revision "any"
    And "inbox" is not unvisited

  Scenario: Check fresh increment
    When we store "$1" into "inbox"
    Then fresh counter is "1" and has revision "any"
    When we store "$2" into "inbox"
    Then fresh counter is "2" and has revision "any"

  Scenario: Store unseen message into inbox
    When we store "$1" into "inbox"
    Then "inbox" is unvisited

  Scenario: Store unseen message into user folder
    When we create "user" folder "foo"
    Then folder named "foo" is not unvisited
    When we store "$1" into folder named "foo"
    Then folder named "foo" is unvisited

  Scenario: Store message with labels into trash [MAILPG-259]
    When we create "user" label "arch"
    And we store "$1" into "trash"
      | label     |
      | user:arch |
    Then in "trash" there is one message
      | mid | label     | revision |
      | $1  | user:arch | 3        |
    And "user" label "arch" has "0" messages at revision "2"

  Scenario Outline: Store unseen message into system folder except inbox
    When we store "$1" into "<folder_type>"
    Then "<folder_type>" is not unvisited

    Examples:
      | folder_type |
      | drafts      |
      | outbox      |
      | sent        |
      | spam        |
      | trash       |

  Scenario: Store write to changelog
    When we store "$1" into "inbox"
    Then in changelog there is
      | revision | type  | mids |
      | 2        | store | $1   |

  Scenario: Store write to changelog with request_info default to null
    When we store "$1" into "inbox"
    Then in changelog there is
      | revision | type  | x_request_id | session_key |
      | 2        | store |              |             |

  Scenario: Store write to changelog with request_info
    When we set request_info "(x-request-id,session-key)"
    And we store "$1" into "inbox"
    Then in changelog there is
      | revision | type  | x_request_id | session_key |
      | 2        | store | x-request-id | session-key |

  @changelog @changed
  Scenario: Store writes valid changed to changelog
    When we store "$1" into "inbox"
    Then last changelog.changed matches "changed/store_message.json" schema

  Scenario: Quiet store write to changelog with quiet flag
    When we quietly store "$1" into "inbox"
    Then in changelog there is
      | revision | type  | quiet |
      | 2        | store | true  |

  Scenario: Store message with attaches
    When we store "$1" into "inbox"
      | attaches                   | flags |
      | 1.2:image/jpg:apple.jpg:10 | seen  |
    Then "inbox" has one attach with "10" size
    And user has one message with attaches, one seen
    When we store "$2" into "inbox"
      | attaches                                               |
      | 1.1:text/plain:War.txt:20 , 1.2:image/png:Peace.png:30 |
    Then "inbox" has "3" attaches with "60" size
    And user has "2" messages with attaches, one seen

  Scenario: Store message with NULL as attaches - MAILPG-309
    When we store "$1" into "inbox"
      | attaches                         |
      | 1.1:text/plain:love-nulls.txt:10 |
    And we store "$2" into "inbox"
      | attaches | flags |
      | NULL     | seen  |
    Then "inbox" has one attach with "10" size
    And user has one message with attaches, not seen

  Scenario: Store into Trash
    When we store "$1" into "trash"
    Then in "trash" there is one message
    And message "$1" has recent doom_date

  Scenario: Store attach into Trash
    When we store "$1" into "trash"
      | attaches                         |
      | 1.1:text/plain:love-nulls.txt:10 |
    Then user has no messages with attaches

  Scenario: Store message before epoch start
    When we store "$Stonebraker" into "inbox"
      | received_date           |
      | 1943-10-11 16:00:00 UTC |
    Then in "inbox" there is one message
      | mid |
      | 1   |

  Scenario: Store message with st_id like 'mulca:2:%'
    When we try store "$1" into "inbox" as "$store"
      | st_id                                                 |
      | mulca:2:21697.62776296.328602695958663276848385379266 |
    Then commit "$store" should produce "StidWithMulca2Prefix"

  Scenario Outline: Store message save threading info
    When we store "$1" into "<folder>"
      | tid | rule       |
      | 42  | references |
    Then in "<folder>" there is one message
      | mid | tid   | found_tid | rule       |
      | $1  | <tid> | 42        | references |

    Examples:
      | folder | tid  |
      | inbox  | 42   |
      | trash  | NULL |

  Scenario: Store message write useful_new_count to changelog
    When we store "$1" into "inbox"
    Then "store" is last changelog entry with "1" as useful_new_count
    When we store "$2" into "trash"
    Then "store" is last changelog entry with "1" as useful_new_count

  @MDB-1015
  Scenario: Store message with NULL attaches should save attaches as NULL
    When we store into "inbox"
      | mid | attaches |
      | $1  | NULL     |
    Then in "inbox" there is one message
      | mid | attaches |
      | $1  | NULL     |

  @MDB-1015
  Scenario: Store message with [] attaches should save attaches as NULL
    When we store into "inbox"
      | mid | attaches |
      | $1  |          |
    Then in "inbox" there is one message
      | mid | attaches |
      | $1  | NULL     |

  Scenario: Store message with mime
    When we store into "inbox"
      | mid | mime                                                                                                                      |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name2:US-ASCII:base64:attachment:file.jpg:cid1:250:500 |
    Then in "inbox" there is one message
      | mid | mime                                                                                                                      |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name2:US-ASCII:base64:attachment:file.jpg:cid1:250:500 |

  Scenario: Store message with NULL mime
    When we store into "inbox"
      | mid | mime |
      | $1  | NULL |
    Then in "inbox" there is one message
      | mid | mime |
      | $1  | NULL |

  Scenario: Store message with empty mime
    When we try store "$1" into "inbox" as "$store"
      | mime |
      |      |
    Then commit "$store" should produce "EmptyMime"

  Scenario: Mailish store
    Given inited mailish user with fid 1
    When we store "$1" into "inbox"
      | mailish                       |
      | 932%1943-10-11 19:00:00+03:00 |
    Then we found "$1" mailish data
      | imap_id | imap_time                 |
      | 932     | 1943-10-11 19:00:00+03:00 |

  Scenario: Mailish async double store
    Given inited mailish user with fid 1
    When we store "$1" into "inbox"
      | mailish                       |
      | 932%1943-10-11 19:00:00+03:00 |
    And we try store "$1" into "inbox" as "$mailish_store"
      | mailish                       |
      | 932%1943-10-11 19:00:00+03:00 |
    Then commit "$mailish_store" should produce "MailishAlreadyExists"

  @other-user
  Scenario: Store do not touch other users
    Given replication stream
    When we store "$1" into "inbox"
    Then there are only our user changes in replication stream


  Scenario: Store message into tabs
    When we store into tab "relevant"
      | mid | flags | size |
      | $1  | seen  | 10   |
      | $2  |       | 20   |
    And we store into tab "news"
      | mid | flags | size |
      | $3  | seen  | 25   |
    And we store into tab "social"
      | mid | flags | size |
      | $4  |       | 15   |
    Then "inbox" has "4" messages, "2" unseen, "70" size at revision "5"
    And tab "relevant" has "2" messages, "1" unseen, "30" size at revision "3"
    And in tab "relevant" there are "2" messages
      | mid | seen  | size |
      | $1  | true  | 10   |
      | $2  | false | 20   |
    And tab "news" has "1" message, no unseen, "25" size at revision "4"
    And in tab "news" there is "1" message
      | mid | seen | size |
      | $3  | true | 25   |
    And tab "social" has "1" message, "1" unseen, "15" size at revision "5"
    And in tab "social" there is "1" message
      | mid | seen  | size |
      | $4  | false | 15   |

  Scenario: Store messages into tabs in one thread
    When we store into tab "relevant"
      | mid | flags | tid |
      | $1  |       | 10  |
    And we store into tab "news"
      | mid | flags | tid |
      | $2  | seen  | 10  |
    And we store into tab "social"
      | mid | flags | tid |
      | $3  |       | 10  |
    Then in "inbox" there is one thread
      | tid | mid | count | unseen |
      | 10  | $3  | 3     | 2      |
    And in tab "relevant" there is one thread
      | tid | mid | count | unseen |
      | 10  | $1  | 3     | 2      |
    And in tab "news" there is one thread
      | tid | mid | count | unseen |
      | 10  | $2  | 3     | 2      |
    And in tab "social" there is one thread
      | tid | mid | count | unseen |
      | 10  | $3  | 3     | 2      |

  Scenario: Store seen message into tab
    When we store "$1" into tab "relevant"
      | flags |
      | seen  |
    Then tab "relevant" is not unvisited
    And tab "relevant" has no fresh

  Scenario: Store unseen message into tab
    When we store "$1" into tab "social"
    Then tab "social" is unvisited
    And tab "social" has "1" fresh

  Scenario: Should increase counter on saving seen message
    When we create "user" label "apple"
    And we accidentally set message_seen to "0" for "user" label "apple"
    And we store into "inbox" 
      | mid | flags        | label      |
      | $1  | seen         | user:apple |
      | $2  |              | user:apple |
    Then "user" label "apple" has "2" messages
    And "user" label "apple" has "1" seen messages
