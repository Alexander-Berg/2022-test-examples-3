Feature: Contacts emails

  Scenario: Create multiple emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    When we create contacts emails
      | email_id | contact_id | email         |
      | $e1      | $1         | foo@yandex.ru |
      | $e2      | $1         | bar@yandex.ru |
      | $e3      | $2         | baz@yandex.ru |
    Then operation result has next revision
    And operation result has advanced by "2" next email_id
    And contacts "next_revision" serial is incremented
    And contacts "next_email_id" serial is advanced by "3"
    And contacts email "$e1" is "foo@yandex.ru"
    And contacts email "$e1" belongs to contact "$1"
    And contacts email "$e2" is "bar@yandex.ru"
    And contacts email "$e2" belongs to contact "$1"
    And contacts email "$e3" is "baz@yandex.ru"
    And contacts email "$e3" belongs to contact "$2"
    And contacts change log ends with
      | revision | type          | x_request_id |
      | 3        | create_emails | tests        |

  Scenario: Delete multiple existing emails
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
      | $e3      | $2         |
    When we delete contacts emails
      | email_id |
      | $e1      |
      | $e2      |
      | $e3      |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts user has no email "$e1"
    And contacts user has no email "$e2"
    And contacts user has no email "$e3"
    And contacts change log ends with
      | revision | type          | x_request_id |
      | 4        | delete_emails | tests        |

  Scenario: Delete nonexistent contacts email
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts emails
      | email_id              |
      | $nonexistent_email_id |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed

  Scenario: Update existing contacts emails label
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id | label |
      | $e1      | $1         | foo   |
      | $e2      | $1         | bar   |
    When we update contacts emails without revision
      | email_id | label |
      | $e1      | foo1  |
      | $e2      | bar1  |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has label "foo1"
    And contacts email "$e1" has next revision
    And contacts email "$e2" has label "bar1"
    And contacts email "$e2" has next revision
    And contacts change log ends with
      | revision | type          | x_request_id |
      | 4        | update_emails | tests        |

  Scenario: Update existing contacts emails with same contact_id
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    When we update contacts emails without revision
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts email "$e1" belongs to contact "$1"
    And contacts email "$e1" has previous revision
    And contacts email "$e2" belongs to contact "$1"
    And contacts email "$e2" has previous revision

  Scenario: Update existing contacts emails type with revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id | type  |
      | $e1      | $1         | [foo] |
      | $e2      | $1         | [bar] |
    And new contacts email "$e1" revision as "$revision"
    When we update contacts emails with "$revision"
      | email_id | type      |
      | $e1      | [foo, my] |
      | $e2      | [bar, my] |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has type "[foo, my]"
    And contacts email "$e1" has next revision
    And contacts email "$e2" has type "[bar, my]"
    And contacts email "$e2" has next revision
    And contacts change log ends with
      | revision | type          | x_request_id |
      | 4        | update_emails | tests        |

  Scenario: Update existing contacts emails contact with outdated revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
      | $2         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts email "$e1" revision as "$revision"
    When we update contacts emails with "$revision"
      | email_id | contact_id |
      | $e1      | $2         |
      | $e2      | $2         |
    And we try update contacts emails with "$revision" as "$op"
      | email_id | contact_id |
      | $e1      | $1         |
    Then commit "$op" should produce "UpdateContactsEmailsWithOutdatedRevision"

  Scenario: Tag contacts emails
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
    When we tag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 6        | tag_emails | tests        |

  Scenario: Tag tagged contacts emails
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
    When we tag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has tag "$tag_id"

  Scenario: Tag partially tagged contacts emails
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
    When we tag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has tag "$tag_id"
    And contacts email "$e2" has tag "$tag_id"
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 7        | tag_emails | tests        |

  Scenario: Tag contacts emails with untagged contacts
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
    When we try tag contacts emails by "$tag_id" as "$op"
      | email_id |
      | $e1      |
      | $e2      |
    Then commit "$op" should produce "TagContactsEmailsWithUntaggedContacts"

  Scenario: Untag contacts emails
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
    When we untag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has no tag "$tag_id"
    And contacts email "$e2" has no tag "$tag_id"
    And contacts change log ends with
      | revision | type         | x_request_id |
      | 7        | untag_emails | tests        |

  Scenario: Untag contacts emails completely
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts
      | contact_id |
      | $1         |
    And new contacts emails
      | email_id | contact_id |
      | $e1      | $1         |
      | $e2      | $1         |
    And new contacts "user" tags
      | tag_id   |
      | $t1      |
      | $t2      |
    And tagged contacts by "$t[1:2]" tags
      | contact_id |
      | $1         |
    And tagged contacts emails by "$t[1:2]" tags
      | email_id |
      | $e1      |
      | $e2      |
    When we untag contacts emails completely
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts email "$e1" has no tag "$t1"
    And contacts email "$e1" has no tag "$t2"
    And contacts email "$e2" has no tag "$t1"
    And contacts email "$e2" has no tag "$t2"
    And contacts change log ends with
      | revision | type         | x_request_id |
      | 10       | untag_emails | tests        |

  Scenario: Untag untagged contacts emails
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
    When we untag contacts emails by "$tag_id"
      | email_id |
      | $e1      |
      | $e2      |
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts email "$e1" has no tag "$tag_id"
    And contacts email "$e2" has no tag "$tag_id"

  Scenario: Untag contacts with tagged contacts emails
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
    When we try untag contacts by "$tag_id" as "$op"
      | contact_id |
      | $1         |
    Then commit "$op" should produce "UntagContactsWithTaggedEmails"
