Feature: purge_archive via shiva util method

  Scenario Outline: Call util purge_archive for different states
    Given new user "<name>"
    And he is in "<state>" state "100" days
    And he is in "<archivation_state>" archivation state "100" days
    When we make util_purge_archive request
    Then shiva responds bad request
    And "<name>" is in "<state>" state
    And "<name>" in main shard is in "<archivation_state>" archivation state

  Examples:
      | name                 | state    | archivation_state    |
      | util_purge_archive_1 | active   | restoration_error    |
      | util_purge_archive_2 | inactive | restoration_complete |
      | util_purge_archive_3 | notified | restoration_complete |
      | util_purge_archive_4 | frozen   | restoration_complete |
      | util_purge_archive_5 | archived | restoration_complete |

  Scenario Outline: Call util purge_archive for purging states
    Given new user "<name>"
    And he is in "active" state "20" days
    And he is in "<archivation_state>" archivation state "2" days
    And s3 will respond without errors for list_objects_v2
    And s3 will respond without errors for delete_objects
    When we make util_purge_archive request
    Then shiva responds ok
    And "<name>" has no archivation data
    And there are no unexpected requests to s3

    Examples:
    | name                  | archivation_state           |
    | util_purge_archive_6  | archivation_in_progress     | 
    | util_purge_archive_7  | restoration_complete        |

