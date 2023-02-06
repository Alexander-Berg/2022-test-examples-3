Feature: Shiva admin requests

  Scenario: Shiva responds pong for ping request
    When we make ping request
    Then shiva responds pong

  Scenario: Shiva returns all shards for shards request
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "shard_name": "first_shard",
            "cluster_id": "cid1",
            "disk_size": 2048,
            "used_size": 48,
            "shard_type": "canary",
            "load_type": "general",
            "can_transfer_to": false,
            "migration": 10,
            "priority": -11
          },
          "2": {
            "shard_id": 2,
            "shard_name": "second_shard",
            "cluster_id": "cid2",
            "disk_size": 1024,
            "used_size": 8,
            "shard_type": "general",
            "load_type": "for_transfer",
            "can_transfer_to": true,
            "migration": 10,
            "priority": 0
          }
      }
      """
    And we make shards request to shiva
    Then shiva returns all this shards

  Scenario: Shiva returns shard by shard_id
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "shard_name": "first_shard",
            "cluster_id": "cid1",
            "disk_size": 2048,
            "shard_type": "canary",
            "load_type": "general",
            "can_transfer_to": false,
            "migration": 10,
            "priority": -11
          },
          "2": {
            "shard_id": 2,
            "shard_name": "second_shard",
            "cluster_id": "cid2",
            "disk_size": 1024,
            "shard_type": "general",
            "load_type": "for_transfer",
            "can_transfer_to": true,
            "migration": 10,
            "priority": 0
          }
      }
      """
    And we make shards request to shiva with "shard_id" "1"
    Then shiva returns "1" shard

  Scenario: Shiva returns shard by cluster_id
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "shard_name": "first_shard",
            "cluster_id": "cid1",
            "disk_size": 2048,
            "shard_type": "canary",
            "load_type": "general",
            "can_transfer_to": false,
            "migration": 10,
            "priority": -11
          },
          "2": {
            "shard_id": 2,
            "shard_name": "second_shard",
            "cluster_id": "cid2",
            "disk_size": 1024,
            "shard_type": "general",
            "load_type": "for_transfer",
            "can_transfer_to": true,
            "migration": 10,
            "priority": 0
          }
      }
      """
    And we make shards request to shiva with "cluster_id" "cid1"
    Then shiva returns "1" shard

  Scenario: Shiva returns shard by shard_type
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "shard_name": "first_shard",
            "cluster_id": "cid1",
            "disk_size": 2048,
            "shard_type": "canary",
            "load_type": "general",
            "can_transfer_to": false,
            "migration": 10,
            "priority": -11
          },
          "2": {
            "shard_id": 2,
            "shard_name": "second_shard",
            "cluster_id": "cid2",
            "disk_size": 1024,
            "shard_type": "general",
            "load_type": "for_transfer",
            "can_transfer_to": true,
            "migration": 10,
            "priority": 0
          }
      }
      """
    And we make shards request to shiva with "shard_type" "general"
    Then shiva returns "2" shard

  Scenario: Shiva adds given shard
    When we make add_shard request to shiva with
      """
      {
          "shard_id": 1,
          "cluster_id": "cid1",
          "disk_size": 2048,
          "shard_type": "canary",
          "shard_name": "name",
          "load_type": "general",
          "can_transfer_to": false,
          "migration": 10,
          "priority": -11
      }
      """
    Then shiva returns 200 OK
    And there is "1" shard in DB
    And shiva returns "1" shard

  Scenario: Shiva adds given shard with default params
    When we make add_shard request to shiva with
      """
      {
          "shard_id": 1,
          "shard_name": "name",
          "cluster_id": "cid1",
          "disk_size": 2048
      }
      """
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "shard_type" "general"
    And "1" shard has "load_type" "general"
    And "1" shard has "can_transfer_to" "False"
    And "1" shard has "migration" "0"
    And "1" shard has "priority" "0"
    And "1" shard has "used_size" "0"

  Scenario: Shiva fails for add_shard request without shard_id argument 
    When we make add_shard request to shiva with
      """
      {
          "cluster_id": "cid1",
          "disk_size": 2048,
          "shard_name": "name",
          "shard_type": "canary",
          "load_type": "general",
          "can_transfer_to": false,
          "migration": 10,
          "priority": -11
      }
      """
    Then shiva returns error with status code "400"
    And there are no shards in DB

  Scenario: Shiva fails for add_shard request without cluster_id argument 
    When we make add_shard request to shiva with
      """
      {
          "shard_id": 1,
          "disk_size": 2048,
          "shard_name": "name",
          "shard_type": "canary",
          "load_type": "general",
          "can_transfer_to": false,
          "migration": 10,
          "priority": -11
      }
      """
    Then shiva returns error with status code "400"
    And there are no shards in DB

  Scenario: Shiva fails for add_shard request without disk_size argument 
    When we make add_shard request to shiva with
      """
      {
          "shard_id": 1,
          "cluster_id": "cid1",
          "shard_name": "name",
          "shard_type": "canary",
          "load_type": "general",
          "can_transfer_to": false,
          "migration": 10,
          "priority": -11
      }
      """
    Then shiva returns error with status code "400"
    And there are no shards in DB

  Scenario: Shiva deletes shard
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048,
            "shard_type": "canary",
            "load_type": "general",
            "can_transfer_to": false,
            "migration": 10,
            "priority": -11
          }
      }
      """
    And we make delete_shard request to shiva with shard_id "1"
    Then shiva returns "1" shard
    And there are no shards in DB

  Scenario: Shiva fails for delete_shard request without shard_id argument
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048
          }
      }
      """
    And we make delete_shard request to shiva without shard_id
    Then shiva returns error with status code "400"
    And there is "1" shard in DB

  Scenario: Shiva fails for update_shard request without shard_id argument
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048
          }
      }
      """
    And we make update_shard request to shiva without shard_id
    Then shiva returns error with status code "400"
    And there is "1" shard in DB

  Scenario: Shiva updates shard cluster_id
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "cluster_id" "cid2"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "cluster_id" "cid2"

  Scenario: Shiva updates shard disk_size
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "disk_size" "4096"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "disk_size" "4096"

  Scenario: Shiva updates shard shard_type
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048,
            "shard_type": "canary"
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "shard_type" "general"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "shard_type" "general"

  Scenario: Shiva updates shard load_type
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048,
            "load_type": "general"
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "load_type" "for_transfer"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "load_type" "for_transfer"

  Scenario: Shiva updates shard can_transfer_to
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",
            "disk_size": 2048,
            "can_transfer_to": false
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "can_transfer_to" "true"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "can_transfer_to" "True"

  Scenario: Shiva updates shard migration
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",         
            "disk_size": 2048,
            "migration": 10          
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "migration" "11"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "migration" "11"

  Scenario: Shiva updates shard priority
    When we make new shiva shards 
      """
      {
          "1": {
            "shard_id": 1,
            "cluster_id": "cid1",         
            "disk_size": 2048,
            "priority": -11
          }
      }
      """
    And we make update_shard request to shiva with shard_id "1" and "priority" "5"
    Then there is "1" shard in DB
    And shiva returns "1" shard
    And "1" shard has "priority" "5"
