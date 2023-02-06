Feature: Auto cleanup via shiva

  Background: Should not have shiva tasks
    Given there are no shiva tasks

  Scenario: cleanup_doomed request should work
    When we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished

  Scenario: Cleanup doomed when message doom date has come
    Given new user "<user>" with messages in "inbox"
    And she has folder "<folder_name>" with type "<folder>"
    And she moved messages from "inbox" to "<folder>" "<days_ago>" days ago
    When we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" "<folder>" folder is empty

    Examples:
    | user    | folder        | folder_name  | days_ago |
    | Zelda1  | trash         | Trash        | 33       |
    | Zelda2  | spam          | Spam         | 11       |
    | Zelda3  | hidden_trash  | HiddenTrash  | 191      |

  Scenario: Do not remove currently deleted messages
    Given new user "<user>" with messages in "inbox"
    And she has folder "<folder_name>" with type "<folder>"
    And she moved messages from "inbox" to "<folder>" "<days_ago>" days ago
    When we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished
    And "<user>" "<folder>" folder is not empty

    Examples:
    | user    | folder       | folder_name  | days_ago |
    | Impa1   | trash        | Trash        | 29       |
    | Impa2   | spam         | Spam         | 9        |
    | Impa3   | hidden_trash | HiddenTrash  | 180      |

  Scenario: Do not cleanup hidden_trash with enabled admin search
    Given new user "Alexandra" with messages in "inbox"
    And "Alexandra" has enabled admin_search
    And she has folder "hidden_trash" with type "hidden_trash"
    And she moved messages from "inbox" to "hidden_trash" "191" days ago
    When we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished
    And "Alexandra" "hidden_trash" folder is not empty

  Scenario: Cleanup doomed should not touch transfered users
    Given new user "Ruto" with messages in "inbox"
    And she moved messages from "inbox" to "trash" "33" days ago
    When we transfer "Ruto" to different shard
    And we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished
    And "Ruto" "trash" folder is not empty

  Scenario: Cleanup doomed should not touch mailish users
    Given new user "Agitha" with messages in "inbox"
    When she has mailish account entry
    Given she moved messages from "inbox" to "trash" "33" days ago
    When we make cleanup_doomed request
    Then shiva responds ok
    And all shiva tasks finished
    And "Agitha" "trash" folder is not empty
