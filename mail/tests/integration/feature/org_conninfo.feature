Feature: Organization conninfo
  Sharpei provides connect organization conninfo

  Background: Setup sharpei
    Given sharpei is started
    And sharpei response to ping

  Scenario: New organization conninfo
    When we request sharpei for organization conninfo
    Then response is verified by json schema "shard_v3.json"

  Scenario: Existing organization conninfo
    When there is organization in sharddb
    And we request sharpei for organization conninfo
    Then response is verified by json schema "shard_v3.json"
