@MAILDEV-358
Feature: Delete user API and husky-tasks

  Scenario: Delete user should delete user from sharddb and delete delete user maildb
    Given new user in first shard
    When passport requests delete user
    Then last delete user request is successful
    And this request produce "delete_shards_user" task
    And task is successful
    And user is not in shards.users
    And user is in shards.deleted_users
    And passport request produce "delete_mail_user" task
    And task is successful

  Scenario: Delete not is_here user user should delete him from shardb, but do not touch his maildb meta, cause it is unexpected behavior
    Given new user in first shard
    When we mark him as not is_here
    And passport requests delete user
    Then last delete user request is successful
    And this request produce "delete_shards_user" task
    And task is successful
    And user is not in shards.users
    And user is in shards.deleted_users
    And passport request produce "delete_mail_user" task
    And task status is "error"
    And error is "not_supported"

  Scenario: Delete user not existed in maildb should delete user from sharddb and delete_mail_user task should be successful, cause maybe it only task retry
    Given new user uninitialized in mdb
    When passport requests delete user
    Then last delete user request is successful
    And this request produce "delete_shards_user" task
    And task is successful
    And user is not in shards.users
    And user is in shards.deleted_users
    And passport request produce "delete_mail_user" task
    And task is successful

  Scenario: Delete user twice should fail on second request
    Given new user in first shard
    When passport requests delete user
    And passport requests delete user
    Then last delete user request is failed with status "400" and code "4"

  Scenario: Delete nonexistent user should fail on second request
    When passport requests delete nonexistent user
    Then last delete user request is failed with status "404" and code "2"

  Scenario: Delete user right now should delete user from sharddb and delete user from maildb with unix epoch as deleted date
    Given new user in first shard
    When passport requests delete user right now
    Then last delete user request is successful
    And this request produce "delete_shards_user" task
    And task is successful
    And user is not in shards.users
    And user is in shards.deleted_users
    And passport request produce "delete_mail_user" task
    And this task has task args with "epoch" in "deleted_date"
    And task is successful

  Scenario: Should get error on deleting mail user that exists in sharddb
    Given new user
    When we plan delete_mail_user
    Then task status is "error"
    And error is "no_such_user"
