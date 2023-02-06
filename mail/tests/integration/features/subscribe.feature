Feature: Subscribe

  Scenario: Request endpoint /subscribe via http post
    Given nothing
    When we request "subscribe" as post
    Then response code is 405

  Scenario: Request endpoint /subscribe with invalid uid
    Given invalid uid as "subscriber_uid" in request
    When we request "subscribe" with params
      | param                   | value   |
      | destination_folder_path | ["bbs"] |
      | owner_uid               | 234     |
      | shared_folder_fid       | 111     |
    Then response code is 500
    And response has "error" with value containing "sharpei_client"

  Scenario: Request endpoint /subscribe with single folder in path
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Request endpoint /subscribe with imap_unsubscribed
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
      | imap_unsubscribed       | yes    |
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription
    And all subscriber folders are imap_unsubscribed

  Scenario: Request endpoint /subscribe with no folders in path
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value |
      | destination_folder_path | []    |
    Then response code is 400
    And response has "error" with value containing "destination_folder_path is empty"

  Scenario: Request endpoint /subscribe with not shared folder
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "subscribe" with different shared_folder_fid and params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
    Then response code is 400
    And response has "error" with value containing "folder is not shared or does not exist"

  Scenario: Request endpoint /subscribe with multiple folders in path
    Given test owner
    And test subscriber
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value                |
      | destination_folder_path | ["y", "o", "r", "k"] |
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Test endpoint /subscribe idempotency
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]"
    And message was stored in subscribed folder
    When we request "subscribe" with same params
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Test endpoint /subscribe idempotency if not fully subscribed
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscribe into the path "["t1"]" failed before create subscription
    When we request "subscribe" with same params
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Test endpoint /subscribe if subscribers folder already exists and empty
    Given test owner
    And test subscriber with empty folder named "t1"
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Test endpoint /subscribe if subscribers folder already exists and not empty
    Given test owner
    And test subscriber with none-empty folder named "t1"
    And owner has shared folder
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
    Then response code is 400
    And response has "error" with value containing "folders already exist and not empty"

  Scenario: Request endpoint /subscribe with different destination path
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]"
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t2"] |
    Then response code is 200
    And response is ok
    And owner has new subscriber
    And subscriber has new subscription

  Scenario: Test endpoint /subscribe recursive
    Given test owner
    And test subscriber
    And owner has shared folders tree
      | name             | parent  |
      | Root             |         |
      |   Folder1        | Root    |
      |     SubFolder1-1 | Folder1 |
      |     SubFolder1-2 | Folder1 |
      |   Folder2        | Root    |
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
      | recursive               | yes    |
    Then response code is 200
    And response is ok
    And all owner shared folders have new subscriber
    And subscriber has all new subscriptions
    And trees hierarchy match

  Scenario: Test endpoint /subscribe recursive with imap_unsubscribed
    Given test owner
    And test subscriber
    And owner has shared folders tree
      | name             | parent  |
      | Root             |         |
      |   Folder1        | Root    |
      |     SubFolder1-1 | Folder1 |
      |     SubFolder1-2 | Folder1 |
      |   Folder2        | Root    |
    When we request "subscribe" with params
      | param                   | value  |
      | destination_folder_path | ["t1"] |
      | recursive               | yes    |
      | imap_unsubscribed       | yes    |
    Then response code is 200
    And response is ok
    And all owner shared folders have new subscriber
    And subscriber has all new subscriptions
    And trees hierarchy match
    And all subscriber folders are imap_unsubscribed

  Scenario: Test endpoint /subscribe recursive idempotency
    Given test owner
    And test subscriber
    And owner has shared folders tree
      | name             | parent  |
      | Root             |         |
      |   Folder1        | Root    |
      |     SubFolder1-1 | Folder1 |
      |     SubFolder1-2 | Folder1 |
      |   Folder2        | Root    |
    And subscriber is "recursively" subscribed to it into the path "["t1"]"
    And message was stored in subscribed folder
    When we request "subscribe" with same params
    Then response code is 200
    And response is ok
    And all owner shared folders have new subscriber
    And subscriber has all new subscriptions
    And trees hierarchy match

  Scenario: Test endpoint /subscribe recursive idempotency if not fully subscribed
    Given test owner
    And test subscriber
    And owner has shared folders tree
      | name             | parent  |
      | Root             |         |
      |   Folder1        | Root    |
      |     SubFolder1-1 | Folder1 |
      |     SubFolder1-2 | Folder1 |
      |   Folder2        | Root    |
    And subscribe "recursively" into the path "["t1"]" failed before create subscription
    When we request "subscribe" with same params
    Then response code is 200
    And response is ok
    And all owner shared folders have new subscriber
    And subscriber has all new subscriptions
    And trees hierarchy match

  Scenario: Test endpoint /subscribe if already subscribed and terminated
    Given test owner
    And test subscriber
    And owner has shared folder
    And subscriber is subscribed to it into the path "["t1"]"
    When all subscriptions were terminated
    And we request "subscribe" with same params
    Then response code is 400
    And response has "error" with value containing "subscriptions still terminating"
