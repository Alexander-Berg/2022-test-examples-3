Feature: clean_archives via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard
    Given there are no tasks in storage delete queue

  Scenario: clean_archives request should work
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call clean_archives after default purge_ttl(10 days)
    Given new user "<user>"
    And he is in "<user_state>" state "20" days
    And he is in "cleaning_in_progress" archivation state "20" days
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with stids: "1,2,3"
    And s3 will respond without errors for delete_objects for keys: "100"
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" has no archivation data
    And storage delete queue has tasks for stids "1,2,3"
    And there are no unexpected requests to s3

    Examples:
    | user                 | user_state |
    | CleanArchivesActive  | active     |
    | CleanArchivesDeleted | deleted    |


  Scenario: Call clean_archives for shared stids
    Given new user "CleanArchivesSharedStids"
    And he is in "active" state "20" days
    And he is in "cleaning_in_progress" archivation state "20" days
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with shared stids: "1,2,3"
    And s3 will respond without errors for delete_objects for keys: "100"
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanArchivesSharedStids" has no archivation data
    And storage delete queue is empty
    And there are no unexpected requests to s3

  Scenario: Call clean_archives before default clean_ttl(10 days)
    Given new user "<user>"
    And he is in "active" state "<user_state_days>" days
    And he is in "cleaning_in_progress" archivation state "<archivation_state_days>" days
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" in main shard is in "cleaning_in_progress" archivation state
    And storage delete queue is empty
    And there are no unexpected requests to s3

    Examples:
    | user                 | user_state_days | archivation_state_days |
    | CleanArchivesBefore1 | 5               | 20                     |
    | CleanArchivesBefore2 | 20              | 5                      |

  Scenario: Call clean_archives when s3 list_objects_v2 fails
    Given new user "CleanArchivesS3ListFails"
    And he is in "active" state "20" days
    And he is in "cleaning_in_progress" archivation state "20" days
    And s3 will respond with 500 "5" times for list_objects_v2
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanArchivesS3ListFails" in main shard is in "cleaning_in_progress" archivation state
    And there are no unexpected requests to s3

  Scenario: Call clean_archives when s3 get_object fails
    Given new user "CleanArchivesS3GetObjectFails"
    And he is in "active" state "20" days
    And he is in "cleaning_in_progress" archivation state "20" days
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond with 500 "5" times for get_object for key "100"
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanArchivesS3GetObjectFails" in main shard is in "cleaning_in_progress" archivation state
    And there are no unexpected requests to s3

  Scenario: Call clean_archives when s3 delete_objects fails
    Given new user "CleanArchivesS3DeleteFails"
    And he is in "active" state "20" days
    And he is in "cleaning_in_progress" archivation state "20" days
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with shared stids: "1,2,3"
    And s3 will respond with 500 "5" times for delete_objects
    When we make clean_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanArchivesS3DeleteFails" in main shard is in "cleaning_in_progress" archivation state
    And there are no unexpected requests to s3
