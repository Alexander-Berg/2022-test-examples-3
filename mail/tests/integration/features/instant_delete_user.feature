@MAILPG-1655
Feature: Testing instant user deletion from husky api

  Background: Should not have tasks from prev tests cause they can be invalid
    Given there are no tasks in storage delete queue

  Scenario: Basic call delete user
    Given new empty user in first shard
    When we request delete user right now
    And shiva purge_deleted_user tasks successfully executed
    Then user data was deleted

  Scenario: Call delete user for user with messages
    Given new user with messages
    When we request delete user right now
    And shiva purge_deleted_user, purge_storage tasks successfully executed
    Then user data was deleted

  Scenario: Call delete user for user with deleted messages
    Given new user with deleted messages
    When we request delete user right now
    And shiva purge_deleted_user, purge_storage tasks successfully executed
    Then user data was deleted

  Scenario: Call delete user for user with stids in storage delete queue
    Given new user with stids in storage delete queue
    When we request delete user right now
    And shiva purge_deleted_user, purge_storage tasks successfully executed
    Then user data was deleted

  Scenario: Call delete user for user with data in all tables
    Given new user with stids in storage delete queue, deleted messages, messages
    When we request delete user right now
    And shiva purge_deleted_user, purge_storage tasks successfully executed
    Then user data was deleted

  Scenario: Call delete user without right now
    Given new user with messages
    When we request delete user
    And shiva purge_deleted_user, purge_storage tasks successfully executed
    Then user is deleted with purge_date not in past
