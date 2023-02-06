@MAILPG-3770
Feature: Testing unlabel messages task

  Scenario: Task should be successful if labels does not exist
    Given new empty user "Christian"
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"}
      }
      """
    Then task is successful


  Scenario: Task should be successful if label is empty
    Given new empty user "Duncan"
    And user has labels
      | lid         | type   | name         |
      | $some_lid   | system | system_hamon |
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"}
      }
      """
    Then task is successful


  Scenario: Task should unlabel messages in all folders
    Given new empty user "Baxter"
    And user has labels
      | lid         | type   | name         |
      | $some_lid   | system | system_hamon |
    And user has messages
      | mid | lids      | folder |
      | $1  | $some_lid | inbox  |
      | $2  | $some_lid | inbox  |
      | $3  | $some_lid | trash  |
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"}
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | lids |
      | $1  |      |
      | $2  |      |
    And user has messages in "trash"
      | mid | lids |
      | $3  |      |


  Scenario: Task should remove only requested label
    Given new empty user "Lesley"
    And user has labels
      | lid         | type   | name         |
      | $some_lid   | system | system_hamon |
      | $other_lid  | user   | system_hamon |
    And user has messages in "inbox"
      | mid | lids                 |
      | $1  | $some_lid,$other_lid |
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"}
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | lids       |
      | $1  | $other_lid |


  Scenario: Task should only process messages within date range
    Given new empty user "Ethan"
    And user has labels
      | lid         | type   | name         |
      | $some_lid   | system | system_hamon |
    And user has messages in "inbox"
      | mid | lids      | received_date             |
      | $1  | $some_lid | 2016-01-01T00:00:00+03:00 |
      | $2  | $some_lid | 2017-01-01T00:00:00+03:00 |
      | $3  | $some_lid | 2018-01-01T00:00:00+03:00 |
      | $4  | $some_lid | 2019-01-01T00:00:00+03:00 |
      | $5  | $some_lid | 2020-01-01T00:00:00+03:00 |
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"},
        "from_date": "2017-01-01T00:00:00+03:00",
        "to_date": "2019-01-01T00:00:00+03:00"
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | lids      |
      | $1  | $some_lid |
      | $2  |           |
      | $3  |           |
      | $4  | $some_lid |
      | $5  | $some_lid |


  Scenario: Task should unlabel messages more than chunk size
    Given new empty user "Kennet"
    And user has labels
      | lid         | type   | name         |
      | $some_lid   | system | system_hamon |
    And user has messages in "inbox"
      | mid | lids      |
      | $1  | $some_lid |
      | $2  | $some_lid |
      | $3  | $some_lid |
    When we plan "unlabel_messages"
      """
      {
        "label": {"name": "system_hamon", "type": "system"},
        "chunk_size": 1
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | lids |
      | $1  |      |
      | $2  |      |
      | $3  |      |

