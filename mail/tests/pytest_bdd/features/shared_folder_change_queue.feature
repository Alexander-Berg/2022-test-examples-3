Feature: Lookup how we fill shared_folder_change_queue

  Scenario: Subscribe does not add to change queue
    When we initialize new user "Efim"
    And we initialize new user "bbs" with "inbox" shared folder
    And we add "Efim" to "inbox@bbs" subscribers
    Then "Efim" change queue for "inbox@bbs" is empty


  Scenario: Store message
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we store "$1" into "inbox"
    Then it produce "$store-cid"
    And "Efim" change queue for "inbox@bbs" is
         """
         - $store-cid
         """


  Scenario: Store into not shared folder does not add change
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we store "$1" into "drafts"
    Then "Efim" change queue for "inbox@bbs" is empty


  Scenario Outline: <operation_title> to shared folder
    Given "inbox@bbs" shared folder with "Efim-<operation_title>" subscriber
    When we store "$1" into "drafts"
    And we <operation>
    Then it produce "$operation-cid"
    And "Efim-<operation_title>" change queue for "inbox@bbs" is
         """
         - $operation-cid
         """

    Examples:
      | operation_title | operation                                      |
      | Move            | move "$1" to "inbox"                           |
      | Copy            | copy "$1" to "inbox" new message "$1c" appears |


  Scenario Outline: <operation_title> from shared folders
    Given "inbox@bbs" shared folder with "Efim-<operation_title>" subscriber
    When we store "$1" into "inbox"
    Then it produce "$store-cid"
    When we <operation>
    Then it produce "$operation-cid"
    And "Efim-<operation_title>" change queue for "inbox@bbs" is
         """
         - $store-cid
         - $operation-cid
         """

    Examples:
      | operation_title | operation            |
      | Move            | move "$1" to "trash" |
      | Delete          | delete "$1"          |


  Scenario: Update message in shared folder
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we store "$1" into "inbox"
    Then it produce "$store-cid"
    When we set "+seen" on "$1"
    Then it produce "$update-cid"
    And "Efim" change queue for "inbox@bbs" is
         """
         - $store-cid
         - $update-cid
         """


  Scenario: Join threads for messages from shared folder
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we store "$1" into "inbox"
      | tid |
      | 1   |
    Then it produce "$store-cid"
    When we store "$2" into "sent"
      | tid |
      | 2   |
    When we join "1" into "2"
    Then it produce "$join-threads-cid"
    And "Efim" change queue for "inbox@bbs" is
         """
         - $store-cid
         - $join-threads-cid
         """


  Scenario: Store message with discontinued shared folder subscription
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we apply unsubscription action on this subscription
    And we store "$1" into "inbox"
    Then it produce "$store-cid"
    And "Efim" change queue for "inbox@bbs" is empty


  Scenario: Store message with shared folder subscription in clear state
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we apply unsubscription action on this subscription
    And we apply clearing action on this subscription
    And we store "$1" into "inbox"
    Then it produce "$store-cid"
    And "Efim" change queue for "inbox@bbs" is empty

  Scenario: Store message with shared folder subscription in clear-fail state
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we apply unsubscription action on this subscription
    And we apply clearing action on this subscription
    And we mark this subscription failed
    And we store "$1" into "inbox"
    Then it produce "$store-cid"
    And "Efim" change queue for "inbox@bbs" is empty

  Scenario: Store message with terminated shared folder subscription
    Given "inbox@bbs" shared folder with "Efim" subscriber
    When we apply unsubscription action on this subscription
    And we apply clearing action on this subscription
    And we apply termination action on this subscription
    And we store "$1" into "inbox"
    Then it produce "$store-cid"
    And "Efim" change queue for "inbox@bbs" is empty


