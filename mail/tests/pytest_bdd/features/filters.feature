Feature: Filters manipulations

  Scenario: New user has no filters
    Given new initialized user
    Then user has no filters

  Scenario: Create new filter and delete it
    Given new initialized user
    When we create "archive" folder "Archive"
    When we create filter "Move apples to Archive"
      """
      IF from contains apple THEN move to archive
      """
    Then user has one filter
    And filter "Move apples to Archive" is
      """
      IF from contains apple THEN move to archive
      """
    When we delete filter "Move apples to Archive"
    Then user has no filters

  Scenario: Create two filters and delete them
    Given new initialized user
    When we create "archive" folder "Archive"
    When we create filter "Drop apples"
      """
      IF from contains apple THEN move to archive
      """
    Then user has one filter
    When we create filter "Drop oranges"
      """
      IF from contains orange THEN move to archive
      """
    Then user has 2 filters
    When we delete two filters "Drop apples" and "Drop oranges"
    Then user has no filters

  Scenario: Create filters with different types
    Given new initialized user
    When we create "archive" folder "Archive"
    When we create typed filter "Drop apples" with type "user"
      """
      IF from contains apple THEN move to archive
      """
    When we create typed filter "Drop oranges" with type "unsubscribe"
      """
      IF from contains orange THEN move to archive
      """
    When we create typed filter "Drop plums" with type "unsubscribe"
      """
      IF from contains plum THEN move to archive
      """
    Then user has 1 filters of "user" type
    Then user has 2 filters of "unsubscribe" type
    Then user has 3 filters

  Scenario: Create unsubscribe filter
    Given new initialized user
    When we create unsubscribe for "somemail@somedomain.ru" with name "SomeDomain" with type "13"
    Then user has 1 filters of "unsubscribe" type
    And unsubscribe filter "SomeDomain" has 1 actions and 2 conditions

  Scenario: Create duplicate unsubscribe filter
    Given new initialized user
    When we create unsubscribe for "somemail@somedomain.ru" with name "SomeDomain" with type "13"
    And we create unsubscribe for "anothermail@anotherdomain.ru" with name "AnotherDomain" with type "17"
    And we create unsubscribe for "anothermail@anotherdomain.ru" with name "NewAnotherDomain" with type "17"
    Then user has 2 filters of "unsubscribe" type
    And unsubscribe filter "SomeDomain" has 1 actions and 2 conditions
    And unsubscribe filter "AnotherDomain" has 1 actions and 2 conditions

  Scenario: Create then replace filter
    Given new initialized user
    When we create filter "Nokia news"
      """
      IF from matches news@nokia.com THEN movel as system:priority_high
      """
    And we replace filter "Nokia news" with "Delete Nokia messages"
      """
      IF from matches news@nokia.com THEN move to trash
      """
    Then user has one filter
    And filter "Delete Nokia messages" is
      """
      IF from matches news@nokia.com THEN move to trash
      """

  Scenario: Create filter with parameter required verification
    Given new initialized user
    When we create filter "Son marks"
      """
      IF from contains school THEN forward to wife@
      """
    Then user has one filter
    Then filter "Son marks" action "forward to wife@" is not verified
    When we verify filter "Son marks" action "forward to wife@"
    Then filter "Son marks" action "forward to wife@" is verified
    When we create filter "Daughter marks"
      """
      IF from contains university THEN forwardwithstore to wife@
      """
    Then filter "Daughter marks" action "forwardwithstore to wife@" is verified

  @new
  Scenario: Recreate filter
    Given new initialized user
    When we create filter "School"
     """
     IF from contains school THEN forwardwithstore to VERIFIED wife@
     """
    And we replace filter "School" with "University"
     """
     IF from contains university THEN forwardwithstore to wife@
     """
    Then filter "University" action "forwardwithstore to wife@" is verified

  Scenario: Disable and enable filter
    Given new initialized user
    When we create disabled filter "Mary me"
      """
      IF body matches "Mary me"
      THEN move to trash
      """
    Then filter "Mary me" is disabled
    When we enable filter "Mary me"
    Then filter "Mary me" is enabled
    When we disable filter "Mary me"
    Then filter "Mary me" is disabled

  Scenario: Check EXISTS filter
    Given new initialized user
    When we create filter "No body"
      """
      IF NOT body exists
      THEN move to trash
      """
    Then user has one filter

  Scenario: Check message type
    Given new initialized user
    When we create filter "Message type check"
      """
      IF type matches 13
      THEN move to trash
      """
    Then user has one filter

  Scenario: Reorder filters
    Given new initialized user
    When we create filter "8. Motherfucker"
      """
      IF header.track matches 8
      THEN movel as user:track-8
      """
    And we create filter "1. Sol Invictus"
      """
      IF header.track matches 1
      THEN movel as user:track-1
      """
    And we create filter "7. Black Friday"
      """
      IF header.trash matches 7
      THEN movel as user:track-7
      """
    Then user filters are in order
      | 8. Motherfucker |
      | 1. Sol Invictus |
      | 7. Black Friday |
    When we reorder user filters
      | 1. Sol Invictus |
      | 7. Black Friday |
      | 8. Motherfucker |
    Then user filters are in order
      | 1. Sol Invictus |
      | 7. Black Friday |
      | 8. Motherfucker |

  Scenario: Reorder filters while we have unsubscribe and system filters
    Given new initialized user
    When we create filter "Rule one"
      """
      IF header.track matches 8
      THEN movel as user:track-8
      """
    And we create filter "Rule two"
      """
      IF header.track matches 1
      THEN movel as user:track-1
      """
    And we create unsubscribe for "somemail@somedomain.ru" with name "Rule unsubscribe" with type "13"
    And we create base system filters
    Then user filters are in order
      | Rule one |
      | Rule two |
    When we reorder user filters
      | Rule two |
      | Rule one |
    Then user filters are in order
      | Rule two |
      | Rule one |

  Scenario: Trying to reorder filter list with duplications
    Given new initialized user
    When we create filter "Rule one"
      """
      IF header.track matches 8
      THEN movel as user:track-8
      """
    And we create filter "Rule two"
      """
      IF header.track matches 1
      THEN movel as user:track-1
      """
    Then user filters are in order
      | Rule one |
      | Rule two |
    When we reorder user filters
      | Rule two |
      | Rule two |
      | Rule one |
    Then user filters are in order
      | Rule one |
      | Rule two |

  Scenario: Trying to reorder uncompleted filter list
    Given new initialized user
    When we create filter "Rule one"
      """
      IF header.track matches 8
      THEN movel as user:track-8
      """
    And we create filter "Rule two"
      """
      IF header.track matches 1
      THEN movel as user:track-1
      """
    And we create filter "Rule six"
      """
      IF header.trash matches 7
      THEN movel as user:track-7
      """
    Then user filters are in order
      | Rule one |
      | Rule two |
      | Rule six |
    When we reorder user filters
      | Rule six |
      | Rule two |
    Then user filters are in order
      | Rule one |
      | Rule two |
      | Rule six |

  Scenario: Replaced filter inherit original order
    Given new initialized user
    When we create filter "1.1 Take Me To Church"
      """
      IF body contains "I was born sick But I love it"
      THEN move to Hozier
      """
    And we create filter "1.8 Sedated"
      """
      IF body contains "Something isn't right, babe"
      THEN move to Hozier
      """
    And we create filter "2.2 Run"
      """
      IF body contains "Run until you feel your lungs bleeding"
      THEN move to Hozier
      """
    Then user filters are in order
      | 1.1 Take Me To Church |
      | 1.8 Sedated           |
      | 2.2 Run               |
    When we replace filter "1.8 Sedated" with "2.3 Arsonist's Lullabye"
      """
      IF body contains "All you have is your fire And the place you need to reach"
      THEN move to Hozier
      """
    Then user filters are in order
      | 1.1 Take Me To Church   |
      | 2.3 Arsonist's Lullabye |
      | 2.2 Run                 |

  Scenario: Uninitialized user can not create filter
    Given new user
    When we try create filter "foo.bar.baz" as "$op"
      """
      IF header.foo matches bar
      THEN move to baz
      """
    Then commit "$op" should produce "NotInitializedUserError"

  Scenario: Create and delete filter do not touch other users
    Given new initialized user
    And replication stream
    When we create filter "Move apples to Trash"
      """
      IF from contains apple THEN move to trash
      """
    When we delete filter "Move apples to Trash"
    Then there are only our user changes in replication stream
