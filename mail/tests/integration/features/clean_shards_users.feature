Feature: Test for ora2pg.clean_shards_users.py

  Scenario: Remove user not in blackbox
    Given new user absent in blackbox
    When we clean shards.users
    Then user is not in shards.users

  Scenario: Leave user in blackbox
    Given new user
    When we clean shards.users
    Then user is in shards.users
