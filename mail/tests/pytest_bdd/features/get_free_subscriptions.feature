Feature: Set worker_id and get from shared_folders_subscriptions

  Background: There should be no free subscriptions before each scenario
    Given there are no free subscriptions

  Scenario: Get free subscriptions returns subscriptions with worker_id
    Given shared folder with 2 free subscriptions
    And "Dobby" worker
    When we get 2 free subscriptions for "Dobby" worker
    Then all new subscriptions are set to worker "Dobby"

  Scenario: Get free subscriptions does not return more than limit
    Given shared folder with 4 free subscriptions
    When we add "Dobby" worker
    When we get 2 free subscriptions for "Dobby" worker
    Then "Dobby" has 2 new subscriptions
    And there are 2 free subscriptions left

  Scenario: Get free subscriptions returns less than limit if less available
    Given shared folder with 2 free subscriptions
    And "Dobby" worker
    When we get 3 free subscriptions for "Dobby" worker
    Then "Dobby" has 2 new subscriptions
    And there are 0 free subscriptions left

  Scenario: Get free subscriptions raise exception with zero limit
    Given shared folder without subscriptions
    And "Dobby" worker
    When we try get 0 free subscriptions for "Dobby" worker as "$op"
    Then commit "$op" should produce "SubscriptionLimitIsNotPositive"

  @concurrent
  Scenario: Concurrent get free subscriptions
    Given shared folder with 3 free subscriptions
    When we add "Dobby-1" worker
    And we add "Dobby-2" worker
    And we try get 2 free subscriptions for "Dobby-1" worker as "$first"
    And we try get 2 free subscriptions for "Dobby-2" worker as "$second"
    And we commit "$first"
    And we commit "$second"
    Then subscriptions in "$first" and "$second" are different
    And there are 0 free subscriptions left

  Scenario: Get free subscriptions do not return not operaible subscriptions
    Given shared folder with one free subscription
    And we apply unsubscription, clearing, termination on this subscription
    And "Dobby" worker
    When we get 100500 free subscriptions for "Dobby" worker
    Then "Dobby" has 0 new subscriptions
