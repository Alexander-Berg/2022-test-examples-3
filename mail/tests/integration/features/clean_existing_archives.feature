Feature: clean_existing_archives via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard
    Given there are no tasks in storage delete queue

  Scenario: clean_existing_archives request should work
    When we make clean_existing_archives request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call clean_existing_archives for archived user with existing archive
    Given new user "CleanExistingArchive"
    And he is in "archived" state "1" days
    And he is in "archivation_complete" archivation state "1" days
    And his archive has "3" messages
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with stids: "1,2,3"
    And s3 will respond without errors for delete_objects for keys: "100"
    When we make clean_existing_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanExistingArchive" has empty archive
    And storage delete queue has tasks for stids "1,2,3"
    And there are no unexpected requests to s3

  Scenario: Call clean_existing_archives for archived user with empty archive
    Given new user "CleanEmptyArchive"
    And he is in "archived" state "1" days
    And he is in "archivation_complete" archivation state "1" days
    And his archive has "0" messages
    When we make clean_existing_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanEmptyArchive" has empty archive
    And there are no unexpected requests to s3

  Scenario: Call clean_existing_archives for archived user with shared stids
    Given new user "CleanExistingArchiveSharedStids"
    And he is in "archived" state "1" days
    And he is in "archivation_complete" archivation state "1" days
    And his archive has "3" messages
    And s3 will respond without errors for list_objects_v2 with keys: "100"
    And s3 will respond without errors for get_object for key "100" with shared stids: "1,2,3"
    And s3 will respond without errors for delete_objects for keys: "100"
    When we make clean_existing_archives request
    Then shiva responds ok
    And all shiva tasks finished
    And "CleanExistingArchiveSharedStids" has empty archive
    And storage delete queue is empty
    And there are no unexpected requests to s3
