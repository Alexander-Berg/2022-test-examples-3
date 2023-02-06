Feature: Quick save message

  Background: All on new user
    Given new initialized user

  Scenario: Quick save message writes in changelog
    When we store "$mid" into "drafts"
      | size |
      | 10   |

    When we quick save "$mid" with
      | size |
      | 1000 |

    Then in changelog there are
      | revision | type       | mids |
      | 3        | quick-save | $mid |


  Scenario: Quick save message changes received_date, attributes, pop_uidl
    When we store "$mid" into "drafts"
      | received_date           | pop_uidl   |
      | 2015-08-19 15:54:46 UTC | 1234567890 |

    When we quick save "$mid" with
      | received_date           | pop_uidl   | attributes |
      | 2016-01-10 16:00:00 UTC | 0987654321 | postmaster |

    Then in "drafts" there is one message
      | mid  | received_date           | pop_uidl   | attributes |
      | $mid | 2016-01-10 16:00:00 UTC | 0987654321 | postmaster |


  Scenario: Quick save message changes hdr_date, firstline, hdr_message_id
    When we store "$mid" into "drafts"
      | hdr_date                | firstline       | hdr_message_id |
      | 2015-08-19 15:54:46 UTC | "My first line" | 1234567890     |

    When we quick save "$mid" with
      | hdr_date                | firstline          | hdr_message_id |
      | 2016-01-10 16:00:00 UTC | "My new firstline" | 0987654321     |

    Then in "drafts" there is one message
      | mid  | hdr_date                | firstline          | hdr_message_id |
      | $mid | 2016-01-10 16:00:00 UTC | "My new firstline" | 0987654321     |


  Scenario: Quick save message changes stid, size, attaches
    When we store "$mid" into "drafts"
      | size | st_id      | attaches                     |
      | 1024 | 1234567890 | 1.1:plain/text:file.name:999 |

    When we quick save "$mid" with
      | size   | st_id      | attaches                     |
      | 100000 | 0987654321 | 1.1:image/jpg:file.jpg:99999 |

    Then in "drafts" there is one message
      | mid  | st_id      | size   | attaches                     |
      | $mid | 0987654321 | 100000 | 1.1:image/jpg:file.jpg:99999 |
    And in "drafts" there is one thread
      | tid  | attach_count | attach_size |
      | $mid | 1            | 99999       |
    And "drafts" has "1" attaches with "99999" size
    And "drafts" has "1" messages, "100000" size


  Scenario: Quick save message changes recipients
    When we store "$mid" into "drafts"
      | recipients                                             |
      | from::vasya@coldmail.moc, reply-to::vasya@coldmail.moc |

    When we quick save "$mid" with
      | recipients                                                                          |
      | from:Vasya:vasya@coldmail.moc, reply-to:Vasya:vasya@coldmail.moc, to:Petya:pt@ml.cm |

    Then in "drafts" there is one message
      | mid  | recipients                                                                          |
      | $mid | from:Vasya:vasya@coldmail.moc, reply-to:Vasya:vasya@coldmail.moc, to:Petya:pt@ml.cm |

  Scenario: Quick save message changes mime
    When we store "$mid" into "drafts"
      | mime                                               |
      | 1:multipart:mixed:--boundary::UTF8:binary::::0:100 |
    When we quick save "$mid" with
      | mime                                                                                                                       |
      | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name1:US-ASCII:base64:attachment:name1.jpg:cid1:150:500 |
    Then in "drafts" there is one message
      | mid  | mime                                                                                                                       |
      | $mid | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name1:US-ASCII:base64:attachment:name1.jpg:cid1:150:500 |

  Scenario: Quick save message with NULL mime
    When we store "$mid" into "drafts"
      | mime                                               |
      | 1:multipart:mixed:--boundary::UTF8:binary::::0:100 |
    When we quick save "$mid" with
      | mime |
      | NULL |
    Then in "drafts" there is one message
      | mid  | mime |
      | $mid | NULL |

  Scenario: Quick save message with empty mime
    When we store "$mid" into "drafts"
      | mime                                               |
      | 1:multipart:mixed:--boundary::UTF8:binary::::0:100 |
    When we try quick save "$mid" as "$quicksave"
      | mime |
      |      |
    Then commit "$quicksave" should produce "EmptyMime"

  Scenario: Quick save messages set newest_tif
    When we store "$m1" into "inbox"
      | tid | received_date           |
      | 42  | 1980-01-01 16:00:00 UTC |
    And we store "$m2" into "drafts"
      | tid | received_date           |
      | 42  | 1990-02-02 16:00:00 UTC |
    And we store "$m3" into "drafts"
      | tid | received_date           |
      | 42  | 1993-03-03 16:00:00 UTC |
    And we store "$m4" into "inbox"
      | tid | received_date           |
      | 42  | 1994-04-04 16:00:00 UTC |
    Then in "drafts" there is one thread
      | tid | mid | newest_mid |
      | 42  | $m3 | $m4        |
    When we quick save "$m3" with
      | received_date           |
      | 1995-05-05 16:00:00 UTC |
    Then in "drafts" there is one thread
      | tid | mid | newest_mid |
      | 42  | $m3 | $m3        |

  Scenario: Quick save message writes in changelog with request_info default to null
    When we store "$mid" into "drafts"
      | size |
      | 10   |
    When we quick save "$mid" with
      | size |
      | 1000 |
    Then in changelog there are
      | revision | type       | x_request_id | session_key |
      | 3        | quick-save |              |             |

  Scenario: Quick save message writes in changelog with request_info
    When we store "$mid" into "drafts"
      | size |
      | 10   |
    When we set request_info "(x-request-id,session-key)"
    And we quick save "$mid" with
      | size |
      | 1000 |
    Then in changelog there are
      | revision | type       | x_request_id | session_key |
      | 3        | quick-save | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Quick save message writes useful_new_count to changelog
    When we store "$1, $2" into "inbox"
    And we store "$3" into "drafts"
      | size |
      | 10   |
    And we quick save "$3" with
      | size |
      | 1000 |
    Then "quick-save" is last changelog entry with "2" as useful_new_count

  @changelog @changed
  Scenario: Quick save message writes valid changed to changelog
    When we store "$1" into "inbox"
    And we quick save "$1" with
      | size |
      | 1000 |
    Then last changelog.changed matches "changed/quick_save_message.json" schema

  @concurrent
  Scenario: Quick save delete message use concurrent operations in this case, cause quick-save steps can not use deleted messages
    When we store "$1" into "inbox"
    And we try delete "$1" as "$del"
    And we try quick save "$1" as "$quick"
    When we commit "$del"
    And we commit "$quick"
    Then "$quick" result has one row with
      """
      updated: False
      """
    And "$quick" result has unchanged revision

  @other-user
  Scenario: Quick save message do not touch other users
    When we store "$1" into "inbox"
    And we setup replication stream
    When we quick save "$1" with
      | size |
      | 1000 |
    Then there are only our user changes in replication stream
