Feature: Contacts tag

  Scenario: Create system contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "system" tag "Foo"
    Then operation result has next revision
    And operation result has next tag_id
    And contacts "next_revision" serial is incremented
    And contacts "next_tag_id" serial is incremented
    And contacts user has "system" tag "Foo"
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 2        | create_tag | tests        |

  Scenario: Create user contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "user" tag "Foo"
    Then contacts user has "user" tag "Foo"

  Scenario: Create duplicate system contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "system" tag "Bar"
    And we try create contacts "system" tag "Bar" as "$op"
    Then commit "$op" should produce "DuplicateContactsTag"

  Scenario: Create duplicate user contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "system" tag "Bar"
    And we try create contacts "system" tag "Bar" as "$op"
    Then commit "$op" should produce "DuplicateContactsTag"

  Scenario: Delete existing contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    When we delete contacts tag "$tag_id"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts user has no "user" tag "Foo"
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 3        | delete_tag | tests        |

  Scenario: Delete nonexistent contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts tag "$nonexistent_tag_id"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed

  Scenario: Delete default system contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we try delete contacts tag "$invited_tag_id" as "$op"
    Then commit "$op" should produce "DeleteDefaultContactsTag"

  Scenario: Update existing contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    When we update contacts tag "$tag_id" name to "Bar" without revision
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts tag "$tag_id" has name "Bar"
    And contacts tag "$tag_id" has next revision
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 3        | update_tag | tests        |

  Scenario: Update existing contacts tag with same name
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    When we update contacts tag "$tag_id" name to "Foo" without revision
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts tag "$tag_id" has name "Foo"
    And contacts tag "$tag_id" has previous revision

  Scenario: Update default contacts tag
    Given new initialized user
    And new contacts "passport_user" user
    When we try update contacts tag "$invited_tag_id" name to "Bar" as "$op"
    Then commit "$op" should produce "UpdateDefaultContactsTag"

  Scenario: Update existing contacts tag with actual revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    And new contacts "$tag_id" tag revision as "$tag_revision"
    When we update contacts tag "$tag_id" name to "Bar" with "$tag_revision"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts tag "$tag_id" has name "Bar"
    And contacts tag "$tag_id" has next revision
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 3        | update_tag | tests        |

  Scenario: Update existing contacts tag with outdated revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" tag "Foo" as "$tag_id"
    And new contacts "$tag_id" tag revision as "$tag_revision"
    When we update contacts tag "$tag_id" name to "Bar" with "$tag_revision"
    And we try update contacts tag "$tag_id" name to "Baz" with "$tag_revision" as "$op"
    Then commit "$op" should produce "UpdateContactsTagWithOutdatedRevision"
