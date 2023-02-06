Feature: Contacts restore

  Scenario: Restore nothing
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we restore contacts to "$revision"
    Then operation result is current revision
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |

  Scenario: Restore create contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts list "$list_id" with type "user" and name "Foo"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has no list "$list_id"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | delete_list | tests         |

  Scenario: Restore restored create contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts list "$list_id" with type "user" and name "Foo"
    And we restore contacts to "$revision"
    And we restore contacts to "$revision"
    Then contacts user has no list "$list_id"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | delete_list | tests         |

  Scenario: Restore restore create contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts list "$list_id" with type "user" and name "Foo"
    And current revision as "$revision_with_list"
    And we restore contacts to "$revision"
    And we restore contacts to "$revision_with_list"
    Then contacts user has list "$list_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | delete_list | tests         |
      | 4        | create_list | tests         |

  Scenario: Restore restore restore create contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts list "$list_id" with type "user" and name "Foo"
    And current revision as "$revision_with_list"
    And we restore contacts to "$revision"
    And we restore contacts to "$revision_with_list"
    And we restore contacts to "$revision"
    Then contacts user has no list "$list_id"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | delete_list | tests         |
      | 4        | create_list | tests         |
      | 5        | delete_list | tests         |

  Scenario: Partially restore restore create contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts list "$list_id" with type "user" and name "Foo"
    And current revision as "$revision_with_list"
    And we create contacts list "$list_id_2" with type "user" and name "Bar"
    And current revision as "$revision_with_list_2"
    And we restore contacts to "$revision"
    And we restore contacts to "$revision_with_list"
    Then contacts user has list "$list_id" with type "user" and name "Foo"
    And contacts user has no list "$list_id_2"
    Then contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | create_list | tests         |
      | 4        | delete_list | tests         |
      | 4        | delete_list | tests         |
      | 5        | create_list | tests         |
      | 5        | create_list | tests         |
      | 5        | delete_list | tests         |

  Scenario: Restore delete contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And current revision as "$revision"
    When we delete contacts list "$list_id"
    And we restore contacts to "$revision"
    Then contacts user has list "$list_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | delete_list | tests         |
      | 4        | create_list | tests         |

  Scenario: Restore update contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And current revision as "$revision"
    When we update contacts list "$list_id" name to "Bar" without revision
    And we restore contacts to "$revision"
    Then contacts user has list "$list_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | update_list | tests         |
      | 4        | update_list | tests         |

  Scenario: Restore delete and update contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And current revision as "$revision"
    When we update contacts list "$list_id" name to "Bar" without revision
    And we delete contacts list "$list_id"
    And we restore contacts to "$revision"
    Then contacts user has list "$list_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | update_list | tests         |
      | 4        | delete_list | tests         |
      | 5        | create_list | tests         |
      | 5        | update_list | tests         |

  Scenario: Restore share contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And current revision as "$revision"
    When we share contacts list "$list_id" to user "42" with type "passport_user"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts list "$list_id" is not shared to "42" with type "passport_user"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | share_list  | tests         |
      | 4        | revoke_list | tests         |

  Scenario: Restore revoke contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts list "$list_id" is shared to "42" with type "passport_user"
    And current revision as "$revision"
    When we revoke contacts list "$list_id" from user "42" with type "passport_user"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts list "$list_id" is shared to "42" with type "passport_user"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_list | tests         |
      | 3        | share_list  | tests         |
      | 4        | revoke_list | tests         |
      | 5        | share_list  | tests         |

  Scenario: Restore subscribe to contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    And current revision as "$revision"
    When we subscribe "$list_id" to user "42" with type "passport_user" contacts list "1"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And "$list_id" is not subscribed to user "42" with type "passport_user" contacts list "1"
    And contacts change log ends with
      | revision | type                   | x_request_id  |
      | 1        | create_user            | register_user |
      | 2        | create_list            | tests         |
      | 3        | subscribe_to_list      | tests         |
      | 4        | revoke_subscribed_list | tests         |

  Scenario: Restore revoke subscribed contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    And new "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"
    And current revision as "$revision"
    When we revoke subscribed "$list_id" to user "42" with type "passport_user" contacts list "1"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"
    And contacts change log ends with
      | revision | type                   | x_request_id  |
      | 1        | create_user            | register_user |
      | 2        | create_list            | tests         |
      | 3        | subscribe_to_list      | tests         |
      | 4        | revoke_subscribed_list | tests         |
      | 5        | subscribe_to_list      | tests         |

  Scenario: Restore create contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts "user" tag "Foo"
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has no "user" tag "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_tag  | tests         |
      | 3        | delete_tag  | tests         |

  Scenario: Restore delete contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    And current revision as "$revision"
    When we delete contacts tag "$tag_id"
    And we restore contacts to "$revision"
    Then contacts user has tag "$tag_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_tag  | tests         |
      | 3        | delete_tag  | tests         |
      | 4        | create_tag  | tests         |

  Scenario: Restore update contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    And current revision as "$revision"
    When we update contacts tag "$tag_id" name to "Bar" without revision
    And we restore contacts to "$revision"
    Then contacts user has tag "$tag_id" with type "user" and name "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
      | 2        | create_tag  | tests         |
      | 3        | update_tag  | tests         |
      | 4        | update_tag  | tests         |

  Scenario: Restore create contacts
    Given new initialized user
    And new contacts "passport_user" user
    And current revision as "$revision"
    When we create contacts
      | contact_id | first_name | last_name |
      | $1         | John       | Doe       |
      | $2         | Foo        | Bar       |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has no contact "$1"
    And contacts user has no contact "$2"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | delete_contacts | tests         |

  Scenario: Restore delete contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And current revision as "$revision"
    When we delete contacts
      | contact_id |
      | $1         |
      | $2         |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has contact "$1"
    And contacts user has contact "$2"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | delete_contacts | tests         |
      | 4        | create_contacts | tests         |

  Scenario: Restore update contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id | first_name | last_name |
      | $1         | John       | Doe       |
      | $2         | Foo        | Bar       |
    And current revision as "$revision"
    When we update contacts without revision
      | contact_id | first_name | last_name |
      | $1         | Peter      | Smith     |
      | $2         | Baz        | Fizz      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contact "$1" has name "John" "Doe"
    And contact "$2" has name "Foo" "Bar"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | update_contacts | tests         |
      | 4        | update_contacts | tests         |

  Scenario: Restore tag contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And current revision as "$revision"
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contact "$1" has no tag "$tag_id"
    And contact "$2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_tag      | tests         |
      | 4        | tag_contacts    | tests         |
      | 5        | untag_contacts  | tests         |

  Scenario: Restore untag contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And current revision as "$revision"
    When we untag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contact "$1" has tag "$tag_id"
    And contact "$2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_tag      | tests         |
      | 4        | tag_contacts    | tests         |
      | 5        | untag_contacts  | tests         |
      | 6        | tag_contacts    | tests         |

  Scenario: Restore tag partially tagged contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And current revision as "$revision"
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contact "$1" has tag "$tag_id"
    And contact "$2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  | changed                          |
      | 1        | create_user     | register_user | {list_ids: [1], tag_ids: [1, 2]} |
      | 2        | create_contacts | tests         | {contact_ids: [1, 2]}            |
      | 3        | create_tag      | tests         | {tag_id: 3}                      |
      | 4        | tag_contacts    | tests         | {contact_ids: [1]}               |
      | 5        | tag_contacts    | tests         | {contact_ids: [2]}               |
      | 6        | untag_contacts  | tests         | {contact_ids: [2]}               |

  Scenario: Restore untag partially untagged contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And current revision as "$revision"
    When we untag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contact "$1" has tag "$tag_id"
    And contact "$2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  | changed                          |
      | 1        | create_user     | register_user | {list_ids: [1], tag_ids: [1, 2]} |
      | 2        | create_contacts | tests         | {contact_ids: [1, 2]}            |
      | 3        | create_tag      | tests         | {tag_id: 3}                      |
      | 4        | tag_contacts    | tests         | {contact_ids: [1]}               |
      | 5        | untag_contacts  | tests         | {contact_ids: [1]}               |
      | 6        | tag_contacts    | tests         | {contact_ids: [1]}               |

  Scenario: Restore create contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And current revision as "$revision"
    When we create contacts emails
      | email_id | contact_id | email         |
      | $e1      | $1         | foo@yandex.ru |
      | $e2      | $1         | bar@yandex.ru |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has no email "$e1"
    And contacts user has no email "$e2"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_emails   | tests         |
      | 4        | delete_emails   | tests         |

  Scenario: Restore delete contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts emails
      | email_id | contact_id | email         |
      | $e1      | $1         | foo@yandex.ru |
      | $e2      | $1         | bar@yandex.ru |
    And current revision as "$revision"
    When we delete contacts emails
      | email_id |
      | $e1      |
      | $e2      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts user has email "$e1"
    And contacts email "$e1" is "foo@yandex.ru"
    And contacts email "$e1" belongs to contact "$1"
    And contacts user has email "$e2"
    And contacts email "$e2" is "bar@yandex.ru"
    And contacts email "$e2" belongs to contact "$1"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_emails   | tests         |
      | 4        | delete_emails   | tests         |
      | 5        | create_emails   | tests         |

  Scenario: Restore update contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts emails
      | email_id | contact_id | label |
      | $e1      | $1         | foo   |
      | $e2      | $1         | bar   |
    And current revision as "$revision"
    When we update contacts emails without revision
      | email_id | label |
      | $e1      | foo1  |
      | $e2      | bar1  |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts email "$e1" has label "foo"
    And contacts email "$e2" has label "bar"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_emails   | tests         |
      | 4        | update_emails   | tests         |
      | 5        | update_emails   | tests         |

  Scenario: Restore tag contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And current revision as "$revision"
    When we tag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts email "$e1" has no tag "$tag_id"
    And contacts email "$e2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type         | x_request_id |
      | 7        | untag_emails | tests        |

  Scenario: Restore untag contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And tagged contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    And current revision as "$revision"
    When we untag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  |
      | 1        | create_user     | register_user |
      | 2        | create_contacts | tests         |
      | 3        | create_emails   | tests         |
      | 4        | create_tag      | tests         |
      | 5        | tag_contacts    | tests         |
      | 6        | tag_emails      | tests         |
      | 7        | untag_emails    | tests         |
      | 8        | tag_emails      | tests         |

  Scenario: Restore tag partially tagged contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And tagged contacts emails by "$tag_id"
      | email_id |
      | $e1      |
    And current revision as "$revision"
    When we tag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  | changed                                         |
      | 1        | create_user     | register_user | {list_ids: [1], tag_ids: [1, 2]}                |
      | 2        | create_contacts | tests         | {contact_ids: [1]}                              |
      | 3        | create_emails   | tests         | {email_ids: [1, 2]}                             |
      | 4        | create_tag      | tests         | {tag_id: 3}                                     |
      | 5        | tag_contacts    | tests         | {contact_ids: [1]}                              |
      | 6        | tag_emails      | tests         | {tagged_emails: [{email_id: 1, contact_id: 1}]} |
      | 7        | tag_emails      | tests         | {tagged_emails: [{email_id: 2, contact_id: 1}]} |
      | 8        | untag_emails    | tests         | {tagged_emails: [{email_id: 2, contact_id: 1}]} |

  Scenario: Restore untag partially untagged contacts emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts "user" tag "Foo" as "$tag_id"
    And tagged contacts by "$tag_id"
      | contact_id |
      | $1         |
    And tagged contacts emails by "$tag_id"
      | email_id |
      | $e1      |
    And current revision as "$revision"
    When we untag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    And we restore contacts to "$revision"
    Then operation result is next revision advanced by "1"
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type            | x_request_id  | changed                                         |
      | 1        | create_user     | register_user | {list_ids: [1], tag_ids: [1, 2]}                |
      | 2        | create_contacts | tests         | {contact_ids: [1]}                              |
      | 3        | create_emails   | tests         | {email_ids: [1, 2]}                             |
      | 4        | create_tag      | tests         | {tag_id: 3}                                     |
      | 5        | tag_contacts    | tests         | {contact_ids: [1]}                              |
      | 6        | tag_emails      | tests         | {tagged_emails: [{email_id: 1, contact_id: 1}]} |
      | 7        | untag_emails    | tests         | {tagged_emails: [{email_id: 1, contact_id: 1}]} |
      | 8        | tag_emails      | tests         | {tagged_emails: [{email_id: 1, contact_id: 1}]} |
