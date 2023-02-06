Feature: Stat
    Sharpei provides information about known mdb shards

    Background: Setup sharpei
      Given sharpei is started
      And sharpei response to ping

    Scenario: Checking that the /stat answer satisfies the schema
      When we request sharpei for stat
      Then response status code is "200"
      And response is verified by json schema "stat.json"

    Scenario: Checking that the /v2/stat answer satisfies the schema
      When we request sharpei for v2/stat
      Then response status code is "200"
      And response is verified by json schema "stat_v2.json"

    Scenario: Checking the /v2/stat answer
      When we request sharpei for v2/stat
      Then response status code is "200"
      And v2/stat response matches all shards

    Scenario: Checking the /v2/stat answer with shard_id
      When we request sharpei for v2/stat with shard_id "2"
      Then response status code is "200"
      And v2/stat response matches shard "2"

    Scenario: Checking the /v2/stat answer with invalid shard_id
      When we request sharpei for v2/stat with shard_id "invalid"
      Then response status code is "400"

    Scenario: Checking the /v2/stat answer with unknown shard_id
      When we request sharpei for v2/stat with shard_id "222"
      Then response status code is "404"

    Scenario: Checking that the /v3/stat answer satisfies the schema
      When we request sharpei for v3/stat
      Then response status code is "200"
      And response is verified by json schema "stat_v3.json"

    Scenario: Checking the /v3/stat answer
      When we request sharpei for v3/stat
      Then response status code is "200"
      And v3/stat response matches all shards

    Scenario: Checking the /v3/stat answer with shard_id
      When we request sharpei for v3/stat with shard_id "2"
      Then response status code is "200"
      And v3/stat response matches shard "2"

    Scenario: Checking the /v3/stat answer with invalid shard_id
      When we request sharpei for v3/stat with shard_id "invalid"
      Then response status code is "400"

    Scenario: Checking the /v3/stat answer with unknown shard_id
      When we request sharpei for v3/stat with shard_id "222"
      Then response status code is "404"

    Scenario: Checking that the /sharddb_stat answer satisfies the schema
      When we request sharpei for sharddb_stat
      Then response status code is "200"
      And response is verified by json schema "sharddb_stat.json"