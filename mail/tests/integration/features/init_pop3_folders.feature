Feature: init_pop3_folders via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: init_pop3_folders request should work
    When we make init_pop3_folder request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Initialize pop3 when have uninitialized pop3 folders
    Given new user "Medli" with messages in "inbox"
    When she enable pop3 for "inbox" folder
    And we make init_pop3_folder request
    Then shiva responds ok
    And all shiva tasks finished
    And "Medli" has pop3 initialized for "inbox"

  Scenario: Initialize pop3 do not touch users in different shards
    Given new user "Saria" with messages in "inbox"
    When she enable pop3 for "inbox" folder
    And we transfer "Saria" to different shard
    And we make init_pop3_folder request
    Then shiva responds ok
    And all shiva tasks finished
    And "Saria" has pop3 uninitialized for "inbox"
