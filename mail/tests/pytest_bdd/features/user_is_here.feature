Feature: User is_there flag

  Scenario: New user always be here
    Given new initialized user
    Then he is here

  Scenario: Transfer user
    Given new initialized user
    When we transfer him to another shard
    Then he is not here

  Scenario Outline: User can not make modifications if he is not here
    Given new initialized user
    When we store "$1" into "inbox"
    And we transfer him to another shard
    Then he is not here
    When we <try_apply_operation>
    Then commit "$op" should produce "UserIsNotHere"

    Examples:
      | try_apply_operation                                             |
      | try store "$2" into "inbox" as "$op"                            |
      | try set "-recent" on "$1" as "$op"                              |
      | try move "$1" to "trash" as "$op"                               |
      | try delete "$1" as "$op"                                        |
      | try copy "$1" to "drafts" as "$op" new message "$2" can appears |
      | reset fresh as "$op"                                            |
      | try create "user" folder "foo" as "$op"                         |
      | try create "user" label "bar" as "$op"                          |
      | try resolve "user" label "bar" as "$op"                         |
