Feature: Contacts

  Scenario: Create multiple contacts
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts
      | contact_id | first_name | last_name | uri |
      | $1         | John       | Doe       | doe |
      | $2         | Foo        | Bar       | bar |
    Then operation result has next revision
    And operation result has advanced by "1" next contact_id
    And contacts "next_revision" serial is incremented
    And contacts "next_contact_id" serial is advanced by "2"
    And contact "$1" has name "John" "Doe"
    And contact "$1" has uri "doe"
    And contact "$2" has name "Foo" "Bar"
    And contact "$2" has uri "bar"
    And contacts change log ends with
      | revision | type            | x_request_id |
      | 2        | create_contacts | tests        |

  Scenario: Delete multiple existing contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    When we delete contacts
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts user has no contact "$1"
    And contacts user has no contact "$2"
    And contacts change log ends with
      | revision | type            | x_request_id |
      | 3        | delete_contacts | tests        |

  Scenario: Delete nonexistent contact
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts
      | contact_id              |
      | $nonexistent_contact_id |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed

  Scenario: Update existing contacts vcard
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id | first_name | last_name |
      | $1         | John       | Doe       |
      | $2         | Foo        | Bar       |
    When we update contacts without revision
      | contact_id | first_name | last_name | uri |
      | $1         | Peter      | Smith     | smi |
      | $2         | Baz        | Fizz      | fiz |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has name "Peter" "Smith"
    And contact "$1" has next revision
    And contact "$1" has uri "smi"
    And contact "$2" has name "Baz" "Fizz"
    And contact "$2" has next revision
    And contact "$2" has uri "fiz"
    And contacts change log ends with
      | revision | type            | x_request_id |
      | 3        | update_contacts | tests        |

  Scenario: Update existing contacts with same format
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id | format   |
      | $1         | vcard_v1 |
      | $2         | vcard_v1 |
    When we update contacts without revision
      | contact_id | format   |
      | $1         | vcard_v1 |
      | $2         | vcard_v1 |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contact "$1" has format "vcard_v1"
    And contact "$1" has previous revision
    And contact "$2" has format "vcard_v1"
    And contact "$2" has previous revision

  Scenario: Update existing contacts list with revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts
      | contact_id | list_id  |
      | $1         | $list_id |
      | $2         | $list_id |
    And new contact "$1" revision as "$revision"
    When we update contacts with "$revision"
      | contact_id | list_id           |
      | $1         | $personal_list_id |
      | $2         | $personal_list_id |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has list "$personal_list_id"
    And contact "$1" has next revision
    And contact "$2" has list "$personal_list_id"
    And contact "$2" has next revision
    And contacts change log ends with
      | revision | type            | x_request_id |
      | 4        | update_contacts | tests        |

  Scenario: Update existing contacts list with outdated revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts
      | contact_id | list_id  |
      | $1         | $list_id |
      | $2         | $list_id |
    And new contact "$1" revision as "$revision"
    When we update contacts with "$revision"
      | contact_id | list_id           |
      | $1         | $personal_list_id |
      | $2         | $personal_list_id |
    And we try update contacts with "$revision" as "$op"
      | contact_id | list_id           |
      | $1         | $personal_list_id |
    Then commit "$op" should produce "UpdateContactsWithOutdatedRevision"

  Scenario: Tag contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has tag "$tag_id"
    And contact "$2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type         | x_request_id |
      | 4        | tag_contacts | tests        |

  Scenario: Tag tagged contacts
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
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contact "$1" has tag "$tag_id"
    And contact "$2" has tag "$tag_id"

  Scenario: Tag partially tagged contacts
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
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has tag "$tag_id"
    And contact "$2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type         | x_request_id |
      | 5        | tag_contacts | tests        |

  Scenario: Untag contacts
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
    When we untag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has no tag "$tag_id"
    And contact "$2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type           | x_request_id |
      | 5        | untag_contacts | tests        |

  Scenario: Untag contacts completely
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tags
      | tag_id   |
      | $t1      |
      | $t2      |
    And tagged contacts by "$t[1:2]" tags
      | contact_id |
      | $1         |
      | $2         |
    When we untag contacts completely
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contact "$1" has no tag "$t1"
    And contact "$1" has no tag "$t2"
    And contact "$2" has no tag "$t1"
    And contact "$2" has no tag "$t2"
    And contacts change log ends with
      | revision | type           | x_request_id |
      | 7        | untag_contacts | tests        |

  Scenario: Untag untagged contacts
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts "user" tag "Foo" as "$tag_id"
    When we untag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contact "$1" has no tag "$tag_id"
    And contact "$2" has no tag "$tag_id"
