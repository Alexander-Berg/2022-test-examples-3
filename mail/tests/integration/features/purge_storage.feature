Feature: purge_storage via shiva

  Background: Should not have tasks from prev tests cause they can be invalid
    Given there are no tasks in storage delete queue

  Scenario: purge_storage request should work
    When we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Delete message from storage
    Given new user "Midna"
    And she has "$this" message in "inbox"
    When "$this" message was purged "10" days ago
    And we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
    And "$this" message does not exist in storage

  @MDB-933
  Scenario: Do not delete message when has actual IMAP copy
    Given new user "Ralis"
    And she has "$this" message in "inbox"
    And "$this" message has "$copy" copy in "drafts"
    When "$this" message was purged "10" days ago
    And we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
    And "$this" message exists in storage

  Scenario: Do not delete message when exists fresh st_id in storage_delete_queue
    Given new user "Ilia"
    And she has "$this" message in "inbox"
    And "$this" message has "$copy" copy in "drafts"
    When "$this" message was purged "10" days ago
    And "$copy" message was purged "2" days ago
    And we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue has task for "$this" message
    And "$this" message exists in storage

  Scenario: Do not delete message when st_id exists in backup.box
    Given new user "Samvel"
    And she has "$this" message in "inbox"
    And user has filled backup for "inbox"
    When "$this" message was purged "10" days ago
    And we put "$this" in storage_delete_queue manually
    And we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
    And "$this" message exists in storage

  Scenario Outline: Do not delete <message_type> message from storage
    Given new user "<user>"
    And she has "<message_type>" "$this" message in "inbox"
    When "$this" message was purged "10" days ago
    And we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
    And "$this" message exists in storage

    Examples:
    | user | message_type  |
    | Azaz | shared        |
    | Bozz | welcome       |

  Scenario: Delete stids in several processes
    Given new user "Jonny" with "5" messages in "inbox"
    And we mark all messages from "inbox" as "$all_inbox"
    When "$all_inbox" messages was purged "10" days ago
    And we make purge_storage request with jobs_count "2" and job_no "0"
    Then shiva responds ok
    And all shiva tasks finished
    And there are some "Jonny" records in storage_delete_queue
    When we make purge_storage request with jobs_count "2" and job_no "1"
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
    And "$all_inbox" messages do not exist in storage

  @MAILPG-1177
  Scenario: With error from mulcagate while deleting message increase fails_count
    Given new user "Kenny"
    And storage delete queue has bad task for message "$invalid"
    When we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue has task for "$invalid" message with "fails_count" increased by 1

  @MAILPG-1177
  Scenario: With error from mulcagate while deleting message increase deleted_date
    Given new user "Eric"
    And storage delete queue has bad task for message "$invalid"
    When we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue has task for "$invalid" message with "deleted_date" increased by 1

  @MAILPG-1177
  Scenario: With error from mulcagate delete task if fails_count greater than max
    Given new user "Timmy"
    And storage delete queue has bad task for message "$invalid"
    And task for message "$invalid" failed 10 times
    When we make purge_storage request
    Then shiva responds ok
    And all shiva tasks finished
    And storage delete queue is empty
