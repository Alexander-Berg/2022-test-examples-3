@dependent-scenarios
Feature: Test deploy v2 on one of the databases

  Background: Wait until internal api is ready
    Given Internal API is up and running
    And actual MetaDB version
    And actual DeployDB version
    And Deploy API is up and running
    And Fake DBM is up and running
    And s3 is up and running
    And Mlock api is up and running
    And Secrets api is up and running
    And we are working with single PostgreSQL cluster

  @setup
  Scenario: PostgreSQL cluster created successfully
    When we try to create cluster "test_cluster"
    Then response should have status 200
    And generated task is finished within "7 minutes"
    And we are able to log in to "test_cluster" with following parameters:
    """
    user: another_test_user
    password: mysupercooltestpassword11111
    """
    And s3 has bucket for cluster
    And cluster has no pending changes

  @delete
  Scenario: Remove cluster
    Given cluster "test_cluster" is up and running
    When we attempt to remove cluster "test_cluster"
    Then response should have status 200
    And generated task is finished within "3 minutes"
    And in worker_queue exists "postgresql_cluster_delete_metadata" task
    And this task is done
    And autogenerated shard was deleted from Solomon
    And we are unable to find cluster "test_cluster"
    But s3 has bucket for cluster
    And in worker_queue exists "postgresql_cluster_purge" task
    When delayed_until time has come
    Then this task is done
    And s3 has no bucket for cluster