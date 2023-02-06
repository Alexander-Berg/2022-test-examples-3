Feature: Prevew attaches

  Scenario: Store message with attachment
    Given new initialized user
    When we store into "inbox"
      | mid | attaches                      |
      | $1  | 1.2:image/jpg:img001.jpg:1024 |
    Then in "inbox" there is one message
      | mid | attaches                      |
      | $1  | 1.2:image/jpg:img001.jpg:1024 |

  Scenario: Store message with two attachments
    Given new initialized user
    When we store into "inbox"
      | mid | attaches                                                    |
      | $1  | 1.2:image/jpg:img001.jpg:1024,1.3:image/jpg:img002.jpg:2048 |
    Then in "inbox" there is one message
      | mid | attaches                                                    |
      | $1  | 1.2:image/jpg:img001.jpg:1024,1.3:image/jpg:img002.jpg:2048 |
