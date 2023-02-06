Feature: make shard requests for all shiva shards via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And current shard is shiva shard with disk size "1000000"
    And there are no active users in shard

  Scenario: Purge deleted messages after specified ttl has expired(shiva get request for all shards)
    Given new user "Holly" with messages in "inbox"
    And he deletes messages from "inbox" "5" days ago
    When we make purge_deleted_box request for all shiva shards with ttl_days "4"
    Then shiva responds ok
    And all shiva tasks finished
    And there are no "Holly" deleted messages
    And there are some "Holly" records in storage_delete_queue

  Scenario: Call space_balancer for overloaded shard(shiva post request for all shards)
    Given new user "Dolly" with "10" messages in "inbox"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make space_balancer request for all shiva shards
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Dolly"

  Scenario: Call space_balancer for overloaded shard with params(shiva post request for all shards)
    Given new user "Molly" with "10" messages in "inbox"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make space_balancer request for all shiva shards with task args {"force": true}
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Molly" with task args containing {"force": true}
