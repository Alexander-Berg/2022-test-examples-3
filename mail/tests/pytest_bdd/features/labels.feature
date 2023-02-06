Feature: Create new labels

  Scenario: Create new label
    Given new initialized user
    When we create "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         |
      | 2        | label-create |

  Scenario: Create "domain" and "user" label with same name
    Given new initialized user
    When we create "domain" label "jira"
    And we create "user" label "jira"
    Then "domain" label "jira" exists
    And "user" label "jira" exists
    And in changelog there are
      | revision | type         |
      | 2        | label-create |
      | 3        | label-create |

  Scenario: Create label twice
    Given new initialized user
    When we create "domain" label "yaru"
    And we try create "domain" label "yaru" as "$double"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         |
      | 2        | label-create |
    Then commit "$double" should produce "LabelWithNameAndTypeLidAlreadyExists"

  Scenario: [MAILPG-493] Create labels with empty name
    Given new initialized user
    When we try create "user" label "" as "$empty-name"
    Then commit "$empty-name" should produce "LabelWithEmptyName"

  Scenario: Create new label by resolve
    Given new initialized user
    When we resolve "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         |
      | 2        | label-create |

  Scenario: Create new label with request_info default to null by resolve
    Given new initialized user
    When we resolve "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         | x_request_id | session_key |
      | 2        | label-create |              |             |

  Scenario: Create new label with request_info by resolve
    Given new initialized user
    When we set request_info "(x-request-id,session-key)"
    And we resolve "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         | x_request_id | session_key |
      | 2        | label-create | x-request-id | session-key |

  Scenario: Create "domain" and "user" label with same name by resolve
    Given new initialized user
    When we resolve "domain" label "jira"
    And we resolve "user" label "jira"
    Then "domain" label "jira" exists
    And "user" label "jira" exists
    And in changelog there are
      | revision | type         |
      | 2        | label-create |
      | 3        | label-create |

  Scenario: Create label and resolve it
    Given new initialized user
    When we create "domain" label "jira"
    And we resolve "domain" label "jira"
    Then "domain" label "jira" exists
    And in changelog there are
      | revision | type         |
      | 2        | label-create |

  Scenario: Create new label with request_info default to null
    Given new initialized user
    When we create "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         | x_request_id | session_key |
      | 2        | label-create |              |             |

  Scenario: Create new label with request_info
    Given new initialized user
    When we set request_info "(x-request-id,session-key)"
    And we create "domain" label "yaru"
    Then "domain" label "yaru" at revision "2"
    And in changelog there is
      | revision | type         | x_request_id | session_key |
      | 2        | label-create | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Create label write useful_new_count to changelog
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we create "user" label "apple"
    Then "label-create" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Create label do not touch other users
    Given new initialized user
    And replication stream
    When we create "user" label "apple"
    Then there are only our user changes in replication stream

    # update this scenario
    # when add new default label
  Scenario: Check lid on label create
    Given new initialized user
    Then user has "8" labels
    When we create "user" label "apple"
    Then user has "9" labels
    And "user" label "apple" has lid "9"

    # update this scenario
    # when add new default label
  Scenario: Check default labels
    Given new initialized user
    Then he has "8" labels
      | name                      | type   |
      | priority_high             | system |
      | pinned                    | system |
      | mute                      | system |
      | answered                  | system |
      | forwarded                 | system |
      | draft                     | system |
      | 12                        | type   |
      | remindme_threadabout:mark | system |

  Scenario: Delete empty label
    Given new initialized user
    When we create "user" label "jira"
    And we delete "user" label "jira"
    Then "user" label "jira" does not exist
    And in changelog there is
      | revision | type         |
      | 3        | label-delete |

  Scenario: Delete non empty label
    Given new initialized user
    When we create "user" label "jira"
    And we store "$1" into "inbox"
    And we set "+user:jira" on "$1"
    And we try to delete "user" label "jira" as "$try_delete_label"
    Then commit "$try_delete_label" should produce "NonEmptyLabelError"
    And "user" label "jira" exists

  Scenario: Delete default empty label
    Given new initialized user
    When we try to delete "system" label "priority_high" as "$try_delete_label"
    Then commit "$try_delete_label" should produce "DefaultLabelError"

  Scenario: Delete not system label with name from default labels
    Given new initialized user
    When we create "user" label "priority_high"
    And we delete "user" label "priority_high"
    Then "user" label "priority_high" does not exist
    And in changelog there is
      | revision | type         |
      | 3        | label-delete |

  Scenario: Delete label after unlabeling messages
    Given new initialized user
    When we create "user" label "jira"
    And we store "$1" into "inbox"
    And we set "+user:jira" on "$1"
    And we set "-user:jira" on "$1"
    And we delete "user" label "jira"
    Then "user" label "jira" does not exist
    And in changelog there is
      | revision | type         |
      | 6        | label-delete |

  Scenario: [MAILPG-259] Delete label with message in trash
    Given new initialized user
    When we create "system" label "ora-emulation"
    And we store "$1" into "inbox"
    And we set "+system:ora-emulation" on "$1"
    Then "system" label "ora-emulation" has one message
    When we move "$1" to "trash"
    Then "system" label "ora-emulation" has "0" messages
    And in "trash" there is one message
      | mid | label                |
      | $1  | system:ora-emulation |
    When we delete "system" label "ora-emulation"
    Then "system" label "ora-emulation" does not exist

  Scenario: Delete label write to changelog with request_info default to null
    Given new initialized user
    When we create "user" label "jira"
    And we delete "user" label "jira"
    Then in changelog there is
      | revision | type         | x_request_id | session_key |
      | 3        | label-delete |              |             |

  Scenario: Delete label write to changelog with request_info
    Given new initialized user
    When we create "user" label "jira"
    And we set request_info "(x-request-id,session-key)"
    And we delete "user" label "jira"
    Then in changelog there is
      | revision | type         | x_request_id | session_key |
      | 3        | label-delete | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Delete label write useful_new_count to changelog
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we create "user" label "apple"
    And we delete "user" label "apple"
    Then "label-delete" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Delete label do not touch other users
    Given new initialized user
    When we create "user" label "apple"
    And we setup replication stream
    And we delete "user" label "apple"
    Then there are only our user changes in replication stream

  Scenario: Update label name
    Given new initialized user
    When we create "user" label "foo"
    And we update "user" label "foo" set name "bar"
    Then "user" label "foo" does not exist
    And "user" label "bar" exists
    And "label-modify" is last changelog entry with one changed element like
       """
       name: bar
       """

  Scenario: Update label color
    Given new initialized user
    When we create "user" label "foo"
    And we update "user" label "foo" set color "red"
    Then "user" label "foo" has color "red"
    And "label-modify" is last changelog entry with one changed element like
       """
       color: red
       """

  Scenario: Update label name and color
    Given new initialized user
    When we create "user" label "foo"
    And we update "user" label "foo" set name "bar" and color "red"
    Then "user" label "foo" does not exist
    And "user" label "bar" exists
    And "user" label "bar" has color "red"
    And "label-modify" is last changelog entry with one changed element like
       """
       color: red
       name: bar
       """

  Scenario: Update label name with request_info default to null
    Given new initialized user
    When we create "user" label "foo"
    And we update "user" label "foo" set name "bar"
    Then in changelog there are
      | revision | type         | x_request_id | session_key |
      | 3        | label-modify |              |             |

  Scenario: Update label name with request_info
    Given new initialized user
    When we create "user" label "foo"
    And we set request_info "(x-request-id,session-key)"
    And we update "user" label "foo" set name "bar"
    Then in changelog there are
      | revision | type         | x_request_id | session_key |
      | 3        | label-modify | x-request-id | session-key |

  @MAILPG-932 @useful_new_count
  Scenario: Update label write useful_new_count to changelog
    Given new initialized user with "$1, $2" in "inbox" and "$3" in "trash"
    When we create "user" label "apple"
    And we update "user" label "apple" set name "banana"
    Then "label-modify" is last changelog entry with "2" as useful_new_count

  @other-user
  Scenario: Update label do not touch other users
    Given new initialized user
    When we create "user" label "apple"
    And we setup replication stream
    And we update "user" label "apple" set name "banana"
    Then there are only our user changes in replication stream
