Feature: Modify shared_folders_subscriptions.state
    # https://wiki.yandex-team.ru/users/shelkovin/sharedfolders/#sostojanijapodpiski
    # https://jing.yandex-team.ru/files/wizard/subscription-4.svg

  Scenario: New subscription has new state
    Given shared folder with subscription
    Then this subscription in "new" state


  Scenario: New subscription has not assigned worker
    Given shared folder with subscription
    Then this subscription has "null" worker_id


  Scenario: Set worker_id for subscription
    Given "Doberman-id" worker
    And shared folder with subscription
    When we assign this subscription to "Doberman-id" worker
    Then this subscription has "Doberman-id" worker_id


  Scenario: Try switch state for not existed subscription
    Given new initialized user with "inbox" shared folder
    When we try apply initialization action on nonexistent subscription as "$op"
    Then commit "$op" should produce "NotExistedSubscription"


  Scenario: State transition update subscription.updated
    Given shared folder with subscription
    And this subscription is assigned to worker
    When we try apply initialization action as "$init"
    And we try apply synchronization action as "$sync"
    And we commit "$init"
    And we commit "$sync"
    Then updated in "$init" result is less than in "$sync" result


  Scenario Outline: Transitions from new state
    Given shared folder with subscription
    And this subscription is assigned to worker
    When we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act            | state        |
      | initialization | init         |
      | unsubscription | discontinued |


  Scenario Outline: Transitions from init state
    Given shared folder with subscription after initialization
    When we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act             | state        |
      | initialization  | init         |
      | synchronization | sync         |
      | unsubscription  | discontinued |


  Scenario Outline: Transitions from sync state
    Given shared folder with subscription after initialization, synchronization
    When we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act             | state        |
      | synchronization | sync         |
      | migration       | migrate      |
      | unsubscription  | discontinued |


  Scenario Outline: Transitions from migrate state
    Given shared folder with subscription after initialization, synchronization, migration
    When we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act         | state            |
      | migration   | migrate          |
      | termination | migrate-finished |


  Scenario Outline: Transitions from discontinued state
    Given shared folder with subscription assigned to worker
    When we apply unsubscription action on this subscription
    And we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act            | state        |
      | unsubscription | discontinued |
      | clearing       | clear        |

  Scenario Outline: Transitions from clear state
    Given shared folder with subscription after unsubscription, clearing
    When we apply <act> action on this subscription
    Then this subscription in "<state>" state

    Examples:
      | act         | state      |
      | clearing    | clear      |
      | termination | terminated |

  Scenario Outline: Invalid transitions from new state
    Given shared folder with subscription
    When we try apply <act> action as "$op"
    Then commit "$op" should produce "InvalidSubscriptionStateTransition"

    Examples:
      | act             |
      | synchronization |
      | migration       |
      | termination     |

  Scenario Outline: Invalid transitions
    Given shared folder with subscription after <applied_actions>
    When we try apply <act> action as "$op"
    Then commit "$op" should produce "InvalidSubscriptionStateTransition"

    Examples:
      | applied_actions                            | act             |
      | initialization                             | migration       |
      | initialization                             | clearing        |
      | initialization                             | termination     |
      | initialization, synchronization            | initialization  |
      | initialization, synchronization            | clearing        |
      | initialization, synchronization            | termination     |
      | initialization, synchronization, migration | initialization  |
      | initialization, synchronization, migration | unsubscription  |
      | initialization, synchronization, migration | clearing        |
      | unsubscription                             | initialization  |
      | unsubscription                             | synchronization |
      | unsubscription                             | migration       |
      | unsubscription                             | termination     |
      | unsubscription, clearing                   | initialization  |
      | unsubscription, clearing                   | synchronization |
      | unsubscription, clearing                   | migration       |
      | unsubscription, clearing                   | unsubscription  |
      | unsubscription, clearing, termination      | initialization  |
      | unsubscription, clearing, termination      | synchronization |
      | unsubscription, clearing, termination      | migration       |
      | unsubscription, clearing, termination      | unsubscription  |
      | unsubscription, clearing, termination      | clearing        |


  @concurrent
  Scenario: Concurrent subscription transitions to same state
    Given shared folder with subscription after initialization
    When we try apply synchronization action as "$first-sync"
    And we try apply synchronization action as "$second-sync"
    And we commit "$first-sync"
    And we commit "$second-sync"
    Then this subscription in "sync" state
    And "$first-sync" is same result as "$second-sync"


  @concurrent
  Scenario: Concurrent subscription transitions to different states
    Given shared folder with subscription after initialization
    When we try apply unsubscription action as "$unsubscribe"
    And we try apply synchronization action as "$sync"
    When we commit "$unsubscribe"
    Then commit "$sync" should produce "InvalidSubscriptionStateTransition"


  Scenario: Mark subscription as failed with fail_reason
    Given shared folder with subscription after initialization
    When we mark this subscription failed cause "it's raining today"
    Then this subscription in "init-fail" state
    And this subscription has "it's raining today" fail_reason


  Scenario: Mark subscription as failed change subscription.updated
    Given shared folder with subscription after initialization
    When we try apply synchronization action as "$sync"
    And we commit "$sync"
    When we try mark this subscription failed as "$fail"
    And we commit "$fail"
    Then updated in "$sync" result is less than in "$fail" result


  Scenario Outline: Mark subscription as failed
    Given shared folder with subscription after <applied_actions>
    When we mark this subscription failed
    Then this subscription in "<state>" state

    Examples:
      | applied_actions                            | state        |
      | initialization                             | init-fail    |
      | initialization, synchronization            | sync-fail    |
      | initialization, synchronization, migration | migrate-fail |
      | unsubscription, clearing                   | clear-fail   |


  @other-user
  Scenario: Mark subscription as failed touch only shared folder user
    Given shared folder with subscription after initialization, synchronization
    And replication stream
    When we mark this subscription failed
    Then there are only our user changes in replication stream

  Scenario Outline: Mark subscription as failed from new state
    Given shared folder with subscription
    When we try mark this subscription failed as "$op"
    Then commit "$op" should produce "InvalidSubscriptionStateTransition"

  Scenario Outline: Mark subscription as failed from unsupported state
    Given shared folder with subscription after <applied_actions>
    When we try mark this subscription failed as "$op"
    Then commit "$op" should produce "InvalidSubscriptionStateTransition"

    Examples:
      | applied_actions                       |
      | unsubscription                        |
      | unsubscription, clearing, termination |


  @concurrent
  Scenario: Concurrent mark as failed
    Given shared folder with subscription after initialization
    When we try mark this subscription failed as "$first"
    And we try mark this subscription failed as "$second"
    And we commit "$first"
    And we commit "$second"
    Then this subscription in "init-fail" state


  @concurrent
  Scenario: Concurrent move to sync and fail
    Given shared folder with subscription after initialization
    When we try apply synchronization action as "$sync"
  # cause we don't commit $sync yet
    Then this subscription in "init" state
    When we try mark this subscription failed as "$fail"
    And we commit "$sync"
    Then this subscription in "sync" state
    When we commit "$fail"
    Then this subscription in "sync-fail" state


  Scenario: Release subscription set worker_id to null
    Given shared folder with subscription assigned to worker
    When we release this subscription
    Then this subscription worker_id is NULL

  Scenario: Release subscription change subscription.updated
    Given shared folder with subscription assigned to worker
    When we try apply initialization action as "$first"
    And we commit "$first"
    When we try release this subscription as "$second"
    And we commit "$second"
    Then updated in "$first" result is less than in "$second" result

  Scenario: Release subscription with wrong worker_id raises exception
    Given shared folder with subscription assigned to worker
    When we try release this subscription from different worker as "$op"
    Then commit "$op" should produce "NotExistedSubscription"


  Scenario: Remove subscription from shared folder subscriptions
    Given shared folder with subscription
    When we delete this subscription
    Then shared folder does not have this subscription

  Scenario: Remove subscription returns this subscription
    Given shared folder with subscription
    When we delete this subscription
    Then deleted subscription is returned

  Scenario: Remove subscription updates revision
    Given shared folder with subscription
    Then global revision is "3"
    When we delete this subscription
    Then global revision is "4"

  Scenario: Remove not existed subscription remove nothing and does not increment revision
    Given shared folder with subscription
    Then global revision is "3"
    When we delete this subscription
    Then global revision is "4"
    When we delete same subscription
    Then nothing is returned
    And global revision is "4"

  Scenario: Remove subscription writes to changelog
    Given shared folder with subscription
    When we delete this subscription
    Then "shared-folder-unsubscribe" is last changelog entry

  Scenario: Remove subscription affects only owner data
    Given shared folder with subscription
    And replication stream
    When we delete this subscription
    Then there are only our user changes in replication stream

  Scenario: Remove subscription does not write to change_queue
    When we initialize new user "Oracle" with "inbox" shared folder
    When we initialize new user "Tom"
    And we subscribe "Tom" to "inbox@Oracle"
    And we initialize new user "Jerry"
    And we subscribe "Jerry" to "inbox@Oracle"
    And we delete "Jerry" subscription to "inbox@Oracle"
    Then "Tom" change queue for "inbox@Oracle" is empty


  Scenario Outline: Unsubscribe from failed subscriptions
    Given shared folder with subscription after <applied_actions>
    When we mark this subscription failed
    Then this subscription in "<state>" state
    When we apply unsubscription action on this subscription
    Then this subscription in "discontinued" state

    Examples:
      | applied_actions                 | state     |
      | initialization                  | init-fail |
      | initialization, synchronization | sync-fail |
