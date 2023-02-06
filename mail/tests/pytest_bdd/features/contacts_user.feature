Feature: Contacts user

  Scenario: Create contacts user while not here
    Given new initialized user
    And new contacts "passport_user" user
    When we mark contacts user as moved from here
    And we create contacts "passport_user" user
    Then operation result is "shard_is_occupied_by_user"
    And contacts user exists
    And contacts user is not here
    And contacts user has "personal" list "Personal"
    And contacts user has "system" tag "Phone"
    And contacts user has "system" tag "Invited"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |
    

  Scenario: Create contacts user while already created
    Given new initialized user
    And new contacts "passport_user" user
    When we create contacts "passport_user" user
    Then operation result is "already_created"
    And contacts user exists
    And contacts user is here
    And contacts user is not deleted
    And contacts user has "personal" list "Personal"
    And contacts user has "system" tag "Phone"
    And contacts user has "system" tag "Invited"
    And contacts change log ends with
      | revision | type        | x_request_id  |
      | 1        | create_user | register_user |

  Scenario: Reinit deleted contacts user
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts user
    And we create contacts "passport_user" user
    Then operation result is "success"
    And contacts user exists
    And contacts user is here
    And contacts user is not deleted
    And contacts user has "personal" list "Personal"
    And contacts user has "system" tag "Phone"
    And contacts user has "system" tag "Invited"
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 2        | create_user | tests        |

  Scenario: Delete existing contacts user
    Given new initialized user
    And new contacts "passport_user" user
    When we delete contacts user
    Then contacts user exists
    And contacts user is deleted
    And contacts user has serials
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 2        | delete_user | tests        |

  Scenario: Delete contacts user while deleting mail user
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
    And new contacts "user" tag "Foo" as "$tag_id"
    When we tag contacts by "$tag_id"
      | contact_id |
      | $1         |
      | $2         |
    And we delete user
    Then contacts user exists
    And contacts user is deleted
    And contacts user has serials
    And contacts user is purged and inited
    And contacts change log ends with
      | revision | type        | x_request_id |
      | 6        | delete_user | delete_user  |
