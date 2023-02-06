Feature: folder_archivation via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: folder_archivation request should work
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: folder_archivation on clean rule should delete messages older than ttl
    Given new user "Harvey"
    And he has "clean" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Harvey" "inbox" folder is empty

  @MAILPG-1762
  Scenario: folder_archivation on clean rule should delete older messages out of max_size limit
    Given new user "Goofy"
    And he has "clean" rule for "inbox" with "3" max message count
    When he received the messages
      | mid  | folder | days_ago |
      | mid1 | inbox  | 20       |
      | mid2 | inbox  | 12       |
      | mid3 | inbox  | 10       |
      | mid4 | inbox  | 5        |
      | mid5 | inbox  | 1        |
    And we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "inbox" does not contain messages with mids "mid1, mid2"
    And "inbox" contains messages with mids "mid3, mid4, mid5"

  Scenario: folder_archivation on clean rule should not delete messages newer than ttl
    Given new user "Oswald"
    And he has "clean" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "20" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Oswald" "inbox" folder is not empty

  Scenario: folder_archivation on clean rule should not delete messages from other folder
    Given new user "Victor"
    And he has "clean" rule for "sent" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Victor" "inbox" folder is not empty

  Scenario: folder_archivation on clean rule should run repeatedly
    Given new user "Waylon"
    And he has "clean" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    When he received one message in "inbox" "40" days ago
    And we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Waylon" "inbox" folder is empty

  Scenario: folder_archivation on archive rule should move messages older than ttl to year-named folder
    Given new user "Edward"
    And he has "archive" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Edward" "inbox" folder is empty
    And "Edward" "inbox" has year-named subfolder with messages

  @MAILPG-1762
  Scenario: folder_archivation on archive rule should archive older messages out of max_size limit
    Given new user "Dragula"
    And he has "archive" rule for "inbox" with "3" max message count
    When he received the messages
      | mid  | folder | days_ago |
      | mid1 | inbox  | 12       |
      | mid2 | inbox  | 12       |
      | mid3 | inbox  | 10       |
      | mid4 | inbox  | 5        |
      | mid5 | inbox  | 1        |
    And we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "inbox" year-named subfolder contains messages with mids "mid1, mid2"
    And "inbox" contains messages with mids "mid3, mid4, mid5"


  Scenario: folder_archivation on archive rule should not move messages newer than ttl
    Given new user "Jonathan"
    And he has "archive" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "20" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Jonathan" "inbox" folder is not empty
    And "Jonathan" "inbox" has no subfolders

  Scenario: folder_archivation on archive rule should not move messages from other folder
    Given new user "Hugo"
    And he has "archive" rule for "sent" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Hugo" "inbox" folder is not empty
    And "Hugo" "inbox" has no subfolders

  Scenario: folder_archivation on archive rule should create shared folders if parent is shared
    Given new user "Floyd"
    And "Floyd" "inbox" is shared
    And he has "archive" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Floyd" "inbox" folder is empty
    And "Floyd" "inbox" has year-named shared subfolder with messages

  Scenario: folder_archivation on archive rule should run repeatedly
    Given new user "Cyrus"
    And he has "archive" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    When he received one message in "inbox" "40" days ago
    And we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Cyrus" "inbox" folder is empty
    And "Cyrus" "inbox" has year-named subfolder with messages

  Scenario: folder_archivation on archive rule for shared should run repeatedly
    Given new user "Garfield"
    And "Garfield" "inbox" is shared
    And he has "archive" rule for "inbox" with "30" days ttl
    And he received one message in "inbox" "40" days ago
    When we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    When he received one message in "inbox" "40" days ago
    And we make folder_archivation request
    Then shiva responds ok
    And all shiva tasks finished
    And "Garfield" "inbox" folder is empty
    And "Garfield" "inbox" has year-named shared subfolder with messages
