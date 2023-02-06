Feature: Store deleted messages
  Background: All on new user
    Given new initialized user

  Scenario: Store deleted message does not increase counters 
    When we store deleted "$1"
    Then fresh counter is "0" and has revision "any"
    And "inbox" has "0" messages, "0" unseen, "0" recent, "0" size at revision "1"

  Scenario: Store deleted message saves received_date
    When we store deleted with
      | mid | received_date           |
      | $1  | 2015-08-19 15:54:46 UTC |
    Then there are "1" removed messages
      | mid | received_date           |
      | $1  | 2015-08-19 15:54:46 UTC |

  Scenario: Store deleted message saves headers info
    When we store deleted with
      | mid | subject            | firstline                   | hdr_message_id          | hdr_date                |
      | $1  | Weird generic plan | We’ve met a strange problem | FFBAC91E@yandex-team.ru | 2015-08-19 15:54:46 UTC |
    Then there are "1" removed messages
      | mid | subject            | firstline                   | hdr_message_id          | hdr_date                |
      | $1  | Weird generic plan | We’ve met a strange problem | FFBAC91E@yandex-team.ru | 2015-08-19 15:54:46 UTC |

  Scenario: Store deleted message saves recipients info
    When we store deleted with
      | mid | recipients                                                                          |
      | $1  | from:Vasya:vasya@coldmail.moc, reply-to:Vasya:vasya@coldmail.moc, to:Petya:pt@ml.cm |
    Then there are "1" removed messages
      | mid | recipients                                                                          |
      | $1  | from:Vasya:vasya@coldmail.moc, reply-to:Vasya:vasya@coldmail.moc, to:Petya:pt@ml.cm |

  Scenario: Store deleted message with empty attachment
    When we store deleted with
      | mid | attaches |
      | $1  |          |
    Then there are "1" removed messages
      | mid | attaches |
      | $1  | NULL     |

  Scenario: Store deleted message with NULL attachment
    When we store deleted with
      | mid | attaches |
      | $1  | NULL     |
    Then there are "1" removed messages
      | mid | attaches |
      | $1  | NULL     |

  Scenario: Store deleted message with attachment
    When we store deleted with
      | mid | attaches                      |
      | $1  | 1.2:image/jpg:img001.jpg:1024 |
    Then there are "1" removed messages
      | mid | attaches                      |
      | $1  | 1.2:image/jpg:img001.jpg:1024 |

  Scenario: Store deleted message with two attachments
    When we store deleted with
      | mid | attaches                                                    |
      | $1  | 1.2:image/jpg:img001.jpg:1024,1.3:image/jpg:img002.jpg:2048 |
    Then there are "1" removed messages
      | mid | attaches                                                    |
      | $1  | 1.2:image/jpg:img001.jpg:1024,1.3:image/jpg:img002.jpg:2048 |

  Scenario: Store deleted message saves st_id
    When we store deleted with
      | mid | st_id |
      | $1  | 1.1.1 |
    Then there are "1" removed messages
      | mid | st_id |
      | $1  | 1.1.1 |

  Scenario: Store deleted message with st_id like 'mulca:2:%'
    When we try store "$1" into "inbox" as "$store"
      | st_id                                                 |
      | mulca:2:21697.62776296.328602695958663276848385379266 |
    Then commit "$store" should produce "StidWithMulca2Prefix"

  Scenario: Store deleted message with mime
    When we store deleted with
      | mid | mime                                                                                                                      |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name2:US-ASCII:base64:attachment:file.jpg:cid1:250:500 |
    Then there are "1" removed messages
      | mid | mime                                                                                                                      |
      | $1  | 1:multipart:mixed:--boundary::UTF8:binary::::0:100, 1.1:image:jpg::name2:US-ASCII:base64:attachment:file.jpg:cid1:250:500 |

  Scenario: Store deleted message with NULL mime
    When we store deleted with
      | mid | mime |
      | $1  | NULL |
    Then there are "1" removed messages
      | mid | mime |
      | $1  | NULL |

  Scenario: Store deleted message with empty mime
    When we try store deleted as "$store"
      | mime |
      |      |
    Then commit "$store" should produce "EmptyMime"

  @other-user
  Scenario: Store deleted does not touch other users
    Given replication stream
    When we store deleted "$1"
    Then there are only our user changes in replication stream

  Scenario: Store deleted writes to changelog
    When we store deleted "$1"
    Then in changelog there is
      | revision | type          | mids |
      | 2        | store-deleted | $1   |

  Scenario: Store deleted writes to changelog with request_info default to null
    When we store deleted "$1"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 2        | store-deleted |              |             |

  Scenario: Store deleted writes to changelog with request_info
    When we set request_info "(x-request-id,session-key)"
    And we store deleted "$1"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 2        | store-deleted | x-request-id | session-key |

  @changelog @changed
  Scenario: Store deleted writes valid changed to changelog
    When we store deleted "$1"
    Then last changelog.changed matches "changed/store_deleted_message.json" schema
