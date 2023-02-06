Feature: space_balancer via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks
    And there are no active users in shard

  Scenario: space_balancer request should work
    When we make space_balancer request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Call space_balancer for overloaded shard for transfer to default shards
    Given new user "Villy" with "10" messages in "inbox"
    And current shard is shiva shard with disk size "1000000"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make space_balancer request
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Villy"

  Scenario: Call space_balancer for overloaded shard for transfer to given shards
    Given new user "Victoria" with "10" messages in "inbox"
    And current shard is shiva shard with disk size "1000000"
    And there are some open for transfer shiva shards with "special" load_type
    When we make space_balancer request with "special" load_type
    Then shiva responds ok
    And all shiva tasks finished
    And there are some transfer tasks for "Victoria"

  Scenario: Call space_balancer for overloaded shard when there are no open shards 
    Given new user "Vivian" with "10" messages in "inbox"
    And current shard is shiva shard with disk size "1000000"
    And there are no open for transfer shiva shards with "special" load_type
    When we make space_balancer request with "special" load_type
    Then shiva responds ok
    And all shiva tasks finished
    And there are no transfer tasks for "Vivian"

  Scenario: Call space_balancer for non-overloaded shard
    Given new user "Vally" with "10" messages in "inbox"
    And current shard is shiva shard with disk size "1000000000000"
    And there are some open for transfer shiva shards with "dbaas_hot" load_type
    When we make space_balancer request
    Then shiva responds ok
    And all shiva tasks finished
    And there are no transfer tasks for "Vally"
