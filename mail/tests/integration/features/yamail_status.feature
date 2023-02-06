Feature: Test yamail_status for different sharpei responses

  Background: check if sharpei is mocked
    Given mocked sharpei
    And local hound
    And fake uid


  Scenario: if sharpei response with error
    Given mocked sharpei respond with status "404"
    When we request "yamail_status" from local hound
    Then there is error in response with "7001" code


  Scenario Outline: test different sharpei responses
    Given mocked sharpei respond with "some_shard" shard and "<hosts>" in response
    When we request "yamail_status" from local hound
    Then state for "some_shard" is "<state>"

    Examples:
      | hosts                   | state               |
      | all alive               | read_write          |
      | master dead             | read_only           |
      | master and replica dead | single_replica_only |
      | replica dead            | single_replica      |
      | both replicas dead      | master_only         |
      | all dead                | db_dead             |
