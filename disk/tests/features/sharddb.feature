Feature: Check that migration scripts is well-formed

  Scenario: Apply migration on empty db is no-error
    Given DB name is "sharddb"
      And new DB
     When we migrate to newest version
     Then no error is produced

  Scenario Outline: Migrate on clean db is no-error
    Given DB name is "sharddb"
      And DB version is <before>
      And migration version is <after>
     When we migrate
    Then no error is produced

  Examples:
    | before | after |
    | 27     | 28    |

