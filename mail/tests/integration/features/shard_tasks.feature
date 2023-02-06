Feature: Tests on husky-api for per-shard tasks

  Background: There is empty husky task queue
    Given husky task queue is clean

  Scenario: Call add_shard_task for active users
    Given we have shard "shard10001" with shard_id "10001"
    And in shard "10001" we have users "Raphael,Donatello,Michelangelo,Leonardo"
    And in shard "10001" we have deleted users "Splinter,Shreder"
    When we call add_shard_task with
      """
      {
        "shard_name": "shard10001",
        "task": "apply_data_migrations"
      }
      """
    Then add_shard_task request is successful with tasks_count "4" and users_count "4"
    When we get all husky tasks
    Then every active shard user has one task with params
      """
      {
        "task": "apply_data_migrations",
        "task_args": {},
        "shard_id": 10001,
        "status": "pending"
      }
      """
    And there are no tasks for deleted shard users

  Scenario: Call add_shard_task with specified loaded shard
    Given we have shard "shard10002" with shard_id "10002"
    And in shard "10002" we have users "Raphael,Donatello,Michelangelo,Leonardo"
    And in shard "10002" we have deleted users "Splinter,Shreder"
    When we call add_shard_task with
      """
      {
        "shard_name": "shard10002",
        "task": "apply_data_migrations",
        "loaded_shard_id": 42
      }
      """
    Then add_shard_task request is successful with tasks_count "4" and users_count "4"
    When we get all husky tasks
    Then every active shard user has one task with params
      """
      {
        "task": "apply_data_migrations",
        "task_args": {},
        "shard_id": 42,
        "status": "pending"
      }
      """
    And there are no tasks for deleted shard users

  Scenario: Call add_shard_task for all users including deleted
    Given we have shard "shard10003" with shard_id "10003"
    And in shard "10003" we have users "Raphael,Donatello,Michelangelo,Leonardo"
    And in shard "10003" we have deleted users "Splinter,Shreder"
    When we call add_shard_task with
      """
      {
        "shard_name": "shard10003",
        "task": "apply_data_migrations",
        "with_deleted": true
      }
      """
    Then add_shard_task request is successful with tasks_count "6" and users_count "6"
    When we get all husky tasks
    Then every shard user including deleted has one task with params
      """
      {
        "task": "apply_data_migrations",
        "task_args": {},
        "shard_id": 10003,
        "status": "pending"
      }
      """

  Scenario: Call add_shard_task without shard_name
    When we call add_shard_task with
      """
      {
        "task": "apply_data_migrations"
      }
      """
    Then add_shard_task request is failed with error containing "shard_name"

  Scenario: Call add_shard_task without task type
    When we call add_shard_task with
      """
      {
        "shard_name": "some_shard"
      }
      """
    Then add_shard_task request is failed with error containing "task"

  Scenario: Call add_shard_task with unknown task type
    When we call add_shard_task with
      """
      {
        "shard_name": "some_shard",
        "task": "some_unknown_task"
      }
      """
    Then add_shard_task request is failed with error containing "Task some_unknown_task not supported"

  Scenario: Call add_shard_task with missing mandatory task arg
    When we call add_shard_task with
      """
      {
        "shard_name": "some_shard",
        "task": "transfer",
        "task_args": {"from_db": "some_shard"}
      }
      """
    Then add_shard_task request is failed with error containing "required: ['to_db', 'from_db'] args, found: dict_keys(['from_db']), missing: {'to_db'}"
