Feature: purge_archives via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard

  Scenario: purge_archives request should work
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call purge_archives after default purge_ttl(10 days)
    Given new user "<user>"
    And he is in "active" state "20" days
    And he is in "<archivation_state>" archivation state "20" days
    And s3 will respond without errors for list_objects_v2
    And s3 will respond without errors for delete_objects
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" has no archivation data
    And there are no unexpected requests to s3

    Examples:
    | user                                | archivation_state           |
    | PurgeArchivesArchivationInProgress  | archivation_in_progress     | 
    | PurgeArchivesRestorationComplete    | restoration_complete        |

  Scenario: Call purge_archives before default purge_ttl(10 days)
    Given new user "<user>"
    And he is in "active" state "<user_state_days>" days
    And he is in "archivation_in_progress" archivation state "<archivation_state_days>" days
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" in main shard is in "archivation_in_progress" archivation state
    And there are no unexpected requests to s3

    Examples:
    | user                 | user_state_days | archivation_state_days |
    | PurgeArchivesBefore1 | 5               | 20                     |
    | PurgeArchivesBefore2 | 20              | 5                      |

  Scenario: Call purge_archives for non-purging state
    Given new user "PurgeArchivesRestorationError"
    And he is in "active" state "20" days
    And he is in "restoration_error" archivation state "20" days
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "PurgeArchivesRestorationError" in main shard is in "restoration_error" archivation state
    And there are no unexpected requests to s3

  Scenario: Call purge_archives for active user without archive
    Given new user "PurgeArchivesUserWithoutArchive"
    And he is in "active" state "20" days
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no unexpected requests to s3

  Scenario: Call purge_archives when s3 list_objects_v2 fails
    Given new user "PurgeArchivesS3ListFails"
    And he is in "active" state "20" days
    And he is in "restoration_complete" archivation state "20" days
    And s3 will respond with 500 "5" times for list_objects_v2
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "PurgeArchivesS3ListFails" in main shard is in "restoration_complete" archivation state
    And there are no unexpected requests to s3

  Scenario: Call purge_archives when s3 delete_objects fails
    Given new user "PurgeArchivesS3DeleteFails"
    And he is in "active" state "20" days
    And he is in "restoration_complete" archivation state "20" days
    And s3 will respond without errors for list_objects_v2
    And s3 will respond with 500 "5" times for delete_objects
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "PurgeArchivesS3DeleteFails" in main shard is in "restoration_complete" archivation state
    And there are no unexpected requests to s3

  Scenario: Call purge_archives when s3 delete_objects responds with errors
    Given new user "PurgeArchivesS3DeleteErrors"
    And he is in "active" state "20" days
    And he is in "restoration_complete" archivation state "20" days
    And s3 will respond without errors for list_objects_v2
    And s3 will respond with errors for delete_objects
    When we make purge_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "PurgeArchivesS3DeleteErrors" in main shard is in "restoration_complete" archivation state
    And there are no unexpected requests to s3
