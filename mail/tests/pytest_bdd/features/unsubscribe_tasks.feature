Feature: Unsubscribe tasks for york

  Background: Clearing previous tasks
    Given there are no tasks left


    # Add tasks
  Scenario: Add unsubscribe task actually adds task
    Given new initialized owner
    When we add unsubscribe tasks for this owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    Then there are tasks for owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |


  Scenario: Add unsubscribe task returns task
    Given new initialized owner
    When we add unsubscribe tasks for this owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    Then there is 1 task for owner
    And returned tasks are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |


  Scenario: Add unsubscribe task returns previous added task
    Given new initialized owner
    When we add unsubscribe tasks for this owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    Then returned tasks are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |
    When we add unsubscribe tasks for this owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | other_id        | 1,2        | 666            | 10                  |
    Then there is 1 task for owner
    And returned tasks are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |


  Scenario: Add unsubscribe task for not existed owner throws
    Given new initialized owner
    When we try add unsubscribe tasks for different owner as "$op" with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    Then commit "$op" should produce "NotInitializedUserError"

  @concurrent
  Scenario: Concurrent add unsubscribe task
    Given new initialized owner
    When we try add unsubscribe tasks for this owner as "$first" with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    And we try add unsubscribe tasks for this owner as "$second" with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | other_id        | 1,2        | 666            | 10                  |
    And we commit "$first"
    And we commit "$second"
    Then there is 1 task for owner
    And returned tasks in "$first" are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |
    And returned tasks in "$second" are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | some_request_id | 1,2        | 666            | 10                  |          |


    # Delete tasks
  Scenario: Delete unsubscribe task actually deletes task
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | request_id_1    | 1,2        | 666            | 10                  |
      | request_id_2    | 1,2,3      | 555            | 13                  |
    When we delete task at position 1
    Then there are tasks for owner with params
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | request_id_2    | 1,2,3      | 555            | 13                  |


  Scenario: Delete unsubscribe task deletes task with subscriptions and returns deleted
    Given shared folder with subscription
    And unsubscribe task for this subscription
    When we delete this task
    Then there are no tasks for owner
    And owner has no subscriptions
    And deleted subscriptions returned


    # Get tasks
  Scenario: Get unsubscribe task returns all available tasks
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    When we get 1 tasks with ttl 300 sec
    Then returned tasks are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |


  Scenario: Get unsubscribe tasks updates assigned
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | some_request_id | 1,2        | 666            | 10                  |
    When we get 1 tasks with ttl 300 sec
    Then task assigned was updated


  Scenario: Get unsubscribe tasks does not return more than limit
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | request_id_1    | 1,2        | 666            | 10                  |
      | request_id_2    | 1,2,3      | 555            | 13                  |
      | request_id_3    | 1,2,3      | 111            | 11                  |
    When we get 2 tasks with ttl 300 sec
    Then there are 2 returned tasks


  Scenario: Get unsubscribe tasks does not return fresh assigned tasks
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid | assigned |
      | request_id_1    | 1,2        | 666            | 10                  | now      |
      | request_id_2    | 1,2,3      | 555            | 13                  | before   |
    When we get 2 tasks with ttl 300 sec
    Then returned tasks are
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | request_id_2    | 1,2,3      | 555            | 13                  |


  Scenario Outline: Get unsubscribe tasks returns tasks only for terminated subscriptions
    Given shared folder with subscription after <applied_actions>
    And unsubscribe task for this subscription
    When we get 1 tasks with ttl 300 sec
    Then there are <count> returned tasks

    Examples:
      | applied_actions                            | count |
      | initialization                             | 0     |
      | initialization, synchronization            | 0     |
      | initialization, synchronization, migration | 0     |
      | unsubscription                             | 0     |
      | unsubscription, clearing                   | 0     |
      | unsubscription, clearing, termination      | 1     |


  Scenario: Get unsubscribe tasks if shared folder has multiple subscribers
    Given shared folder with subscription after unsubscription, clearing, termination
    And unsubscribe task for this subscription
    And this shared folder has extra subscription
    When we get 1 tasks with ttl 300 sec
    Then there are 1 returned tasks


  @concurrent
  Scenario: Concurrent get unsubscribe tasks
    Given new initialized owner with tasks
      | task_request_id | owner_fids | subscriber_uid | root_subscriber_fid |
      | request_id_1    | 1,2        | 111            | 11                  |
      | request_id_2    | 1,2,3      | 222            | 12                  |
      | request_id_3    | 2          | 333            | 13                  |
      | request_id_4    | 2,3        | 444            | 14                  |
      | request_id_5    | 1,3        | 555            | 15                  |
    When we try get 3 tasks with ttl 300 sec as "$first"
    And we try get 3 tasks with ttl 300 sec as "$second"
    And we commit "$first"
    And we commit "$second"
    Then there are 5 returned tasks in "$first" and "$second" combined
    And tasks in "$first" and "$second" are different
