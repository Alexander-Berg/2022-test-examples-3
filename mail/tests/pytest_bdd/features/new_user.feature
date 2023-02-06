Feature: New user

  Scenario: initialize new user
    Given new user
    When we initialize user as "$init"
    And we commit "$init"
    Then "$init" result has one row
      | register_user |
      | success       |
    And user has just initialized folders
      | type   |
      | inbox  |
      | spam   |
      | trash  |
      | sent   |
      | outbox |
      | drafts |
    And user has just initialized tabs
      | type     |
      | relevant |
      | news     |
      | social   |
    And "system" label "priority_high" exists
    And in changelog there is
      | revision | type     |
      | 1        | register |

  Scenario: initialize new user with sending welcome mails
    Given new user
    When we initialize user with sending welcome mails
    Then user has just initialized folders
      | type   |
      | spam   |
      | trash  |
      | sent   |
      | outbox |
      | drafts |
    And user has just initialized tabs
      | type   |
      | news   |
      | social |
    And "inbox" is not empty
    And tab "relevant" is not empty
    And in changelog there is
      | revision | type     |
      | 1        | register |

  Scenario: initialize new user with filters
    Given new user
    When we initialize user with filters
    Then user has just initialized folders
      | type   |
      | spam   |
      | trash  |
      | sent   |
      | outbox |
      | drafts |
    And user has just initialized tabs
      | type     |
      | relevant |
      | news     |
      | social   |
    And user has 2 filters of "system" type

  Scenario: initialize new user twice
    Given new initialized user
    When we initialize user as "$init"
    And we commit "$init"
    Then "$init" result has one row
      | register_user      |
      | already_registered |

  Scenario: initialize transferred user
    Given new initialized user
    When we transfer user to another shard
    And we initialize user as "$init"
    And we commit "$init"
    Then "$init" result has one row
      | register_user             |
      | shard_is_occupied_by_user |

  Scenario: initialize deleted user
    Given new initialized user
    When we delete user
    And we initialize user as "$init"
    And we commit "$init"
    Then "$init" result has one row
      | register_user |
      | success       |
    And user has folders initialized at revision "2"
      | type   |
      | inbox  |
      | spam   |
      | trash  |
      | sent   |
      | outbox |
      | drafts |
    And user has tabs initialized at revision "2"
      | type     |
      | relevant |
      | news     |
      | social   |
    And "system" label "priority_high" exists
    And in changelog there is
      | revision | type     |
      | 3        | register |
