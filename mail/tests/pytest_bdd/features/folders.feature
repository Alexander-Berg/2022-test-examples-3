Feature: Create new folders

  Background: New user
    Given new initialized user
    Then "inbox" has not messages at revision "1"

  Scenario: New folder
    When we create "user" folder "apple"
    Then folder named "apple" has not messages at revision "2"

  Scenario: New folder revision increment
    When we store "$1" into "inbox"
    Then "inbox" has one message at revision "2"
    When we create "user" folder "banana"
    Then folder named "banana" has revision "3"

  Scenario: Create folder with inbox type
    When we try create "inbox" folder "folder-with-inbox-type" as "$should-fail"
    Then COMMIT "$should-fail" should produce "FolderTypeAlreadyExists"

  Scenario: Create folder with existed name
    When we create "user" folder "lettuce"
    And we try create "user" folder "lettuce" as "$duplicate"
    Then COMMIT "$duplicate" should produce "FolderAlreadyExists"

  Scenario: Create folder with existed name on different level
    When we create "user" folder "basket"
    When we create "user" folder "apple"
    And we create "user" folder "apple" under "basket"
    Then folder named "basket" is empty
    And folder named "apple" is empty
    And folder named "basket|apple" is empty

  Scenario: Check folder fid
    Then user has "6" folders
    When we create "user" folder "apple"
    Then user has "7" folders
    And folder named "apple" has fid "7" at revision "2"

  Scenario: Delete folder, then create new folder
    When we create "user" folder "apple"
    And we delete folder named "apple"
    Then in changelog there is
      | revision | type          |
      | 3        | folder-delete |
    When we create "user" folder "banana"
    Then folder named "banana" has fid "8" at revision "4"

  Scenario: Delete already deleted
    When we create "user" folder "cherry"
    Then folder named "cherry" has fid "7" at revision "2"
    When we delete folder with fid "7"
    Then global revision is "3"
    When we delete folder with fid "7"
    Then global revision is "3"

  Scenario: Delete default folder
    When we try to delete folder with fid "2" as "$default_folder"
    Then commit "$default_folder" should produce "FolderDeleteDefault"

  Scenario: Delete folder write to changelog with request info default to null
    When we create "user" folder "apple"
    And we delete folder named "apple"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 3        | folder-delete |              |             |

  Scenario: Delete folder write to changelog with request info
    When we create "user" folder "apple"
    And we set request_info "(x-request-id,session-key)"
    And we delete folder named "apple"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 3        | folder-delete | x-request-id | session-key |

  @other-user
  Scenario: Delete folder do not touch other users
    When we create "user" folder "apple"
    And we setup replication stream
    And we delete folder named "apple"
    Then there are only our user changes in replication stream

  Scenario: Create folder write to changelog
    When we create "user" folder "apple"
    Then in changelog there is
      | revision | type          |
      | 2        | folder-create |

  Scenario: Create folder write to changelog with request info default to null
    When we create "user" folder "apple"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 2        | folder-create |              |             |

  Scenario: Create folder write to changelog with request info
    When we set request_info "(x-request-id,session-key)"
    And we create "user" folder "apple"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 2        | folder-create | x-request-id | session-key |


  @MAILPG-932 @useful_new_count
  Scenario: Create folder write useful_new_count
    When we store "$1, $2" into "inbox"
    And we store "$3" into "trash"
    When we create "user" folder "apple"
    Then "folder-create" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Create folder do not touch other users
    Given replication stream
    When we create "user" folder "apple"
    Then there are only our user changes in replication stream


  Scenario: Modify folder write to changelog
    When we create "user" folder "f1"
    When we set name on folder named "f1" to "f2"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 3        | folder-modify |              |             |

  Scenario: Modify folder write to changelog with request_info
    When we create "user" folder "f1"
    And we set request_info "(x-request-id,session-key)"
    And we set name on folder named "f1" to "f2"
    Then in changelog there is
      | revision | type          | x_request_id | session_key |
      | 3        | folder-modify | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Modify folder write useful_new_count
    When we store "$1, $2" into "inbox"
    And we store "$3" into "trash"
    And we create "user" folder "apple"
    And we set name on folder named "apple" to "banana"
    Then "folder-modify" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Modify folder do not touch other users
    When we create "user" folder "apple"
    And we setup replication stream
    And we set name on folder named "apple" to "banana"
    Then there are only our user changes in replication stream

  Scenario: Rename default folder
    When we try to set name on folder fid "1" to "AAA" as "fail"
    Then commit "fail" should produce "UpdateFolderDefault"

  Scenario: Reparent default folder
    When we try to set parent on "inbox" folder named "Inbox" to "2" as "fail"
    Then commit "fail" should produce "UpdateFolderDefault"

  Scenario: Modify folder type
    When we create "user" folder "discount"
    Then unique_type of folder named "discount" is "False"
    When we set type of folder named "discount" to "discount"
    Then in changelog there is
      | revision | type               |
      | 3        | folder-modify-type |
    Then unique_type of folder named "discount" is "True"

  Scenario: Modify folder type with existing type
    When we create "discount" folder "discount"
    When we create "user" folder "discount2"
    And we try set type of folder named "discount2" to "discount" as "$should-fail"
    Then COMMIT "$should-fail" should produce "FolderTypeAlreadyExists"

  Scenario: Modify folder type for default folder
    When we try set type of folder fid "1" to "discount" as "$should-fail"
    Then COMMIT "$should-fail" should produce "FolderTypeChangeDefault"

  Scenario: [MAILPG-493] Create folders with empty name
    When we try create "user" folder "" as "$create-empty-name"
    Then commit "$create-empty-name" should produce "FolderWithEmptyName"

  Scenario: Modify folder type write to change log with request info default to null
    When we create "user" folder "discount"
    And we set type of folder named "discount" to "discount"
    Then in changelog there is
      | revision | type               | x_request_id | session_key |
      | 3        | folder-modify-type |              |             |

  Scenario: Modify folder type write to change log with request info
    When we create "user" folder "discount"
    And we set request_info "(x-request-id,session-key)"
    And we set type of folder named "discount" to "discount"
    Then in changelog there is
      | revision | type               | x_request_id | session_key |
      | 3        | folder-modify-type | x-request-id | session-key |

  Scenario: Get or create not existing folder
    When user do not have folder named "test_name"
    And we get or create "user" folder "test_name"
    Then folder named "test_name" has not messages at revision "2"

  Scenario: Get or create existing folder
    When we create "user" folder "exist_name"
    And we get or create "user" folder "exist_name"
    Then folder named "exist_name" has not messages at revision "2"

