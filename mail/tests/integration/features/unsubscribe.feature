Feature: Unsubscribe

  Scenario: Request endpoint /unsubscribe via http post
    Given nothing
    When we request "unsubscribe" as post
    Then response code is 405

  Scenario: Request endpoint /unsubscribe with invalid uid
    Given invalid uid as "subscriber_uid" in request
    When we request "unsubscribe" with params
      | param                   | value   |
      | owner_uid               | 234     |
      | shared_folder_fid       | 111     |
    Then response code is 500
    And response has "error" with value containing "sharpei_client"

  Scenario: Test endpoint /unsubscribe for single subscribed folder
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]" with messages synced
    When we request "unsubscribe"
    Then unsubscribe was successfull
    And subscriber folders were deleted

  Scenario: Test endpoint /unsubscribe deletes empty user folders
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]" with messages synced
    And subscriber has empty folder "folder" in "t1"
    When we request "unsubscribe"
    Then unsubscribe was successfull
    And subscriber folders were deleted

  Scenario: Test endpoint /unsubscribe saves none-empty user folders
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]" with messages synced
    And subscriber has none-empty folder "folder" in "t1"
    When we request "unsubscribe"
    Then unsubscribe was successfull
    And subscriber folders were not deleted

  Scenario: Test endpoint /unsubscribe does not delete folders if user messages were found
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]" with messages synced
    And message was stored in "t1"
    When we request "unsubscribe"
    Then unsubscribe was successfull
    And subscriber folders were not deleted

  Scenario: Test endpoint /unsubscribe for recursively subscribed folder
    Given test owner
    And test subscriber
    And owner has shared folders tree
      | name             | parent  |
      | Root             |         |
      |   Folder1        | Root    |
      |     SubFolder1-1 | Folder1 |
      |     SubFolder1-2 | Folder1 |
      |   Folder2        | Root    |
    And subscriber is "recursively" subscribed to it into the path "["t1"]" with messages synced
    When we request "unsubscribe"
    Then unsubscribe was successfull
    And subscriber folders were deleted

  Scenario: Test endpoint /usubscribe after previous unsubscribe failed on deleting subscription
    Given test owner
    And test subscriber
    And owner has shared folder
    And previous unsubscribe failed on deleting subscription
    When we request "unsubscribe"
    Then response code is 200
    And response is ok
    And owner has no subscribers

  Scenario: Test endpoint /usubscribe idempotency
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "unsubscribe"
    Then response code is 200
    And response is ok

  Scenario: Test york process unsubscribe task after restart
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]"
    And york was restarted during unsubscribe
    When all subscriptions were terminated
    Then owner has no subscribers
    And subscriber has no subscriptions
    And subscriber folders were deleted

  Scenario: Test york process unsubscribe task after failed on deleting subscription
    Given test owner
    And test subscriber
    And owner has shared folder
    And previous unsubscribe failed on deleting subscription
    And york was restarted during unsubscribe
    When all subscriptions were terminated
    Then owner has no subscribers
