Feature: clean_archive via shiva util method

  Scenario Outline: Call util clean_archive for different states
    Given new user "<name>"
    And he is in "<state>" state "100" days
    And he is in "<archivation_state>" archivation state "100" days
    When we make util_clean_archive request
    Then shiva responds bad request
    And "<name>" is in "<state>" state
    And "<name>" in main shard is in "<archivation_state>" archivation state

  Examples:
      | name                 | state    | archivation_state    |
      | util_clean_archive_1 | active   | restoration_error    |
      | util_clean_archive_2 | deleted  | restoration_error    |
      | util_clean_archive_3 | inactive | cleaning_in_progress |
      | util_clean_archive_4 | notified | cleaning_in_progress |
      | util_clean_archive_5 | frozen   | cleaning_in_progress |
      | util_clean_archive_6 | archived | cleaning_in_progress |

  Scenario Outline: Call util clean_archive for cleaning states
    Given new user "<name>"
    And he is in "<state>" state "20" days
    And he is in "cleaning_in_progress" archivation state "2" days
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with stids: "1,2,3"
    And s3 will respond without errors for delete_objects for keys: "100"
    When we make util_clean_archive request
    Then shiva responds ok
    And "<name>" has no archivation data
    And storage delete queue has tasks for stids "1,2,3"
    And there are no unexpected requests to s3

    Examples:
    | name                  | state   |
    | util_clean_archive_7  | active  | 
    | util_clean_archive_8  | deleted |

