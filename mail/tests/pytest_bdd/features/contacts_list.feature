Feature: Contacts list

  Scenario: Create user contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "user" list "Foo"
    Then operation result has next revision
    And operation result has next list_id
    And contacts "next_revision" serial is incremented
    And contacts "next_list_id" serial is incremented
    And contacts user has "user" list "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 2        | create_list | tests        |

  Scenario: Create user contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "user" list "Foo"
    Then contacts user has "user" list "Foo"

  Scenario: Create duplicate personal contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we try create contacts "personal" list "Bar" as "$op"
    Then commit "$op" should produce "DuplicateUniqueTypeContactsList"

  Scenario: Create duplicate user contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "user" list "Bar"
    And we try create contacts "user" list "Bar" as "$op"
    Then commit "$op" should produce "DuplicateContactsList"

  Scenario: Delete existing contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    When we delete contacts list "$list_id"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts user has no "user" list "Foo"
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 3        | delete_list | tests        |

  Scenario: Delete nonexistent contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts list "$nonexistent_list_id"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed

  Scenario: Delete default system contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we try delete contacts list "$personal_list_id" as "$op"
    Then commit "$op" should produce "DeleteDefaultContactsList"

  Scenario: Update existing contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    When we update contacts list "$list_id" name to "Bar" without revision
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts list "$list_id" has name "Bar"
    And contacts list "$list_id" has next revision
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 3        | update_list | tests        |

  Scenario: Update existing contacts list with same name
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    When we update contacts list "$list_id" name to "Foo" without revision
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts list "$list_id" has name "Foo"
    And contacts list "$list_id" has previous revision

  Scenario: Update default contacts list
    Given new initialized user
    And new contacts "passport_user" user
    When we try update contacts list "$personal_list_id" name to "Bar" as "$op"
    Then commit "$op" should produce "UpdateDefaultContactsList"

  Scenario: Update existing contacts list with actual revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts "$list_id" list revision as "$list_revision"
    When we update contacts list "$list_id" name to "Bar" with "$list_revision"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts list "$list_id" has name "Bar"
    And contacts list "$list_id" has next revision
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 3        | update_list | tests        |

  Scenario: Update existing contacts list with outdated revision
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts "$list_id" list revision as "$list_revision"
    When we update contacts list "$list_id" name to "Bar" with "$list_revision"
    And we try update contacts list "$list_id" name to "Baz" with "$list_revision" as "$op"
    Then commit "$op" should produce "UpdateContactsListWithOutdatedRevision"

  Scenario: Share contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    When we share contacts list "$list_id" to user "42" with type "passport_user"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts list "$list_id" is shared to "42" with type "passport_user"
    And contacts change log ends with
      | revision | type       | x_request_id |
      | 3        | share_list | tests        |

  Scenario: Share shared contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts list "$list_id" is shared to "42" with type "passport_user"
    When we share contacts list "$list_id" to user "42" with type "passport_user"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts list "$list_id" is shared to "42" with type "passport_user"

  Scenario: Revoke contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    And new contacts list "$list_id" is shared to "42" with type "passport_user"
    When we revoke contacts list "$list_id" from user "42" with type "passport_user"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And contacts list "$list_id" is not shared to "42" with type "passport_user"
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 4        | revoke_list | tests        |

  Scenario: Revoke not shared contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "Foo" as "$list_id"
    When we revoke contacts list "$list_id" from user "42" with type "passport_user"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And contacts list "$list_id" is not shared to "42" with type "passport_user"

  Scenario: Subscribe to contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    When we subscribe "$list_id" to user "42" with type "passport_user" contacts list "1"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"
    And contacts change log ends with
      | revision | type              | x_request_id |
      | 3        | subscribe_to_list | tests        |

  Scenario: Subscribe to contacts list already subscribed to
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    And new "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"
    When we subscribe "$list_id" to user "42" with type "passport_user" contacts list "1"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"

  Scenario: Revoke subscribed contacts list
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    And new "$list_id" is subscribed to user "42" with type "passport_user" contacts list "1"
    When we revoke subscribed "$list_id" to user "42" with type "passport_user" contacts list "1"
    Then operation result is next revision
    And contacts "next_revision" serial is incremented
    And "$list_id" is not subscribed to user "42" with type "passport_user" contacts list "1"
    And contacts change log ends with
      | revision | type                   | x_request_id |
      | 4        | revoke_subscribed_list | tests        |

  Scenario: Revoke contacts list not subscribed to
    Given new initialized user
    And new contacts "passport_user" user
    And new contacts "user" list "SubscribedList" as "$list_id"
    When we revoke subscribed "$list_id" to user "42" with type "passport_user" contacts list "1"
    Then operation result is current revision
    And contacts "next_revision" serial is not changed
    And "$list_id" is not subscribed to user "42" with type "passport_user" contacts list "1"
