Feature: Cut firstlines [MAILDEV-1669]

  Scenario: Cut old messages
    Given new user with deleted messages in first shard
    When we cut firstlines for old messages
    Then there is our "cut_firstlines" task
    And task is successful
    And old messages are cut
    And new messages are unchanged

  Scenario: Cut all messages
    Given new user with deleted messages in first shard
    When we cut firstlines for all messages
    Then there is our "cut_firstlines" task
    And task is successful
    And old messages are cut
    And new messages are cut

  Scenario: Cut multiple times
    Given new user with deleted messages in first shard
    When we cut firstlines for all messages
    Then there is our "cut_firstlines" task
    And task is successful
    When we cut firstlines for all messages
    Then there is our "cut_firstlines" task
    And task is successful

  Scenario: Cut old messages during transfer
    Given new user with deleted messages in first shard
    When we plan transfer with cutting old firstlines
    Then there is our "transfer" task
    And task is successful
    And old messages in the new shard are cut
    And new messages in the new shard are unchanged

  Scenario: Cut all messages during transfer
    Given new user with deleted messages in first shard
    When we plan transfer with cutting all firstlines
    Then there is our "transfer" task
    And task is successful
    And old messages in the new shard are cut
    And new messages in the new shard are cut
