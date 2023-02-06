Feature: pg_partman_maintenance via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: pg_partman_maintenance for mail.change_log
    Given there are some partitions for "mail.change_log" in maildb
    When we drop "3" partitions for "mail.change_log" in maildb
    And we make pg_partman_maintenance request for shard
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "mail.change_log" in maildb

  Scenario: pg_partman_maintenance for mail.doberman_jobs_change_log
    Given there are some partitions for "mail.doberman_jobs_change_log" in maildb
    When we drop "3" partitions for "mail.doberman_jobs_change_log" in maildb
    And we make pg_partman_maintenance request for shard
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "mail.doberman_jobs_change_log" in maildb

  Scenario: pg_partman_maintenance for contacts.change_log
    Given there are some partitions for "contacts.change_log" in maildb
    When we drop "3" partitions for "contacts.change_log" in maildb
    And we make pg_partman_maintenance request for shard
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "contacts.change_log" in maildb

  Scenario: pg_partman_maintenance for contacts.equivalent_revisions
    Given there are some partitions for "contacts.equivalent_revisions" in maildb
    When we drop "3" partitions for "contacts.equivalent_revisions" in maildb
    And we make pg_partman_maintenance request for shard
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "contacts.equivalent_revisions" in maildb

  Scenario: pg_partman_maintenance for shiva.shard_tasks_info
    Given there are some partitions for "shiva.shard_tasks_info" in huskydb
    When we drop "3" partitions for "shiva.shard_tasks_info" in huskydb
    And we make pg_partman_maintenance request for huskydb
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "shiva.shard_tasks_info" in huskydb

  Scenario: pg_partman_maintenance for queue.processed_tasks
    Given there are some partitions for "queue.processed_tasks" in queuedb
    When we drop "3" partitions for "queue.processed_tasks" in queuedb
    And we make pg_partman_maintenance request for queuedb
    Then shiva responds ok
    And all shiva tasks finished
    And there are the same partitions for "queue.processed_tasks" in queuedb
