@MAILPG-2325
Feature: Testing move old messages to tabs task

  Scenario: Task should create requested tabs
    Given new empty user "Oswald"
    And user does not have tabs
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "social", "so_type": "101"}
        ]
      }
      """
    Then task is successful
    And user has tabs "relevant, news, social"


  Scenario: Task with final=false should not change can_read_tabs
    Given new user "Edward"
    And user cannot read tabs
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": []
      }
      """
    Then task is successful
    And user cannot read tabs


  Scenario: Task with final=true should set can_read_tabs true
    Given new user "Harvey"
    And user cannot read tabs
    When we plan "move_messages_to_tabs"
      """
      {
        "final": true,
        "mapping": []
      }
      """
    Then task is successful
    And user can read tabs


  Scenario: Task with should fill requested tabs and move the rest to relevant
    Given new empty user "Floyd"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
      | $2  |     |             |
      | $3  |     | $social_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  | relevant |
      | $3  | relevant |


  Scenario: Task should ignore unexisting SO type
    Given new empty user "Hugo"
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "666"}
        ]
      }
      """
    Then task is successful


  Scenario: Task should ignore messages not in inbox
    Given new empty user "Jervis"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "sent"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
      | $2  |     |             |
      | $3  |     | $social_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "social", "so_type": "101"}
        ]
      }
      """
    Then task is successful
    And user has messages in "sent"
      | mid | tab |
      | $1  |     |
      | $2  |     |
      | $3  |     |


  Scenario: Task should ignore already tabbed messages
    Given new empty user "Victor"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab      | lids        |
      | $1  | social   | $news_lid   |
      | $2  | news     |             |
      | $3  | relevant | $social_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "social", "so_type": "101"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | social   |
      | $2  | news     |
      | $3  | relevant |


  Scenario: Task with multiple rules for tab should fill requested tabs
    Given new empty user "Jonathan"
    And user has SO labels
      | lid         | so_type |
      | $news_1_lid | 100     |
      | $news_2_lid | 113     |
    And user has messages in "inbox"
      | mid | tab | lids        |
      | $1  |     | $news_1_lid |
      | $2  |     |             |
      | $3  |     | $news_2_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "news", "so_type": "113"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  | relevant |
      | $3  | news     |


  Scenario: Task with multiple rules should fill tabs with priority
    Given new empty user "Slade"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab | lids                   |
      | $1  |     | $news_lid, $social_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "social", "so_type": "101"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab  |
      | $1  | news |


  Scenario: Task with from_date should update only newer messages
    Given new empty user "Roman"
    And user has SO labels
      | lid         | so_type |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab | lids        | received_date             |
      | $1  |     | $social_lid | 2019-01-01T00:00:00+03:00 |
      | $2  |     |             | 2018-01-01T00:00:00+03:00 |
      | $3  |     | $social_lid | 2017-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "social", "so_type": "101"}
        ],
        "from_date": "2019-01-01T00:00:00+03:00"
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab    |
      | $1  | social |
      | $2  |        |
      | $3  |        |


  Scenario: Task with min_count for tab should update at least min_count of messages, even older than from_date
    Given new empty user "Waylon"
    And user has SO labels
      | lid       | so_type |
      | $news_lid | 100     |
    And user has messages in "inbox"
      | mid | tab | lids      | received_date             |
      | $1  |     | $news_lid | 2019-01-01T00:00:00+03:00 |
      | $2  |     | $news_lid | 2018-01-01T00:00:00+03:00 |
      | $3  |     | $news_lid | 2017-01-01T00:00:00+03:00 |
      | $4  |     | $news_lid | 2016-01-01T00:00:00+03:00 |
      | $5  |     | $news_lid | 2015-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ],
        "tabs_limits": [
          {"type": "news", "min_count": 3}
        ],
        "from_date": "2019-01-01T00:00:00+03:00",
        "chunk_size": 1
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab  |
      | $1  | news |
      | $2  | news |
      | $3  | news |
      | $4  |      |
      | $5  |      |


  Scenario: Task with min_count for tab should update all messages, if less than min_count
    Given new empty user "Basil"
    And user has SO labels
      | lid       | so_type |
      | $news_lid | 100     |
    And user has messages in "inbox"
      | mid | tab | lids      | received_date             |
      | $1  |     | $news_lid | 2019-01-01T00:00:00+03:00 |
      | $2  |     | $news_lid | 2018-01-01T00:00:00+03:00 |
      | $3  |     | $news_lid | 2017-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ],
        "tabs_limits": [
          {"type": "news", "min_count": 10}
        ],
        "from_date": "2019-01-01T00:00:00+03:00",
        "chunk_size": 1
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab  |
      | $1  | news |
      | $2  | news |
      | $3  | news |


  Scenario: Task idempotency
    Given new empty user "Thomas"
    And user has SO labels
      | lid       | so_type |
      | $news_lid | 100     |
    And user has messages in "inbox"
      | mid | tab  | lids      | received_date             |
      | $1  | news |           | 2019-01-01T00:00:00+03:00 |
      | $2  | news |           | 2018-01-01T00:00:00+03:00 |
      | $3  | news |           | 2017-01-01T00:00:00+03:00 |
      | $4  |      | $news_lid | 2016-01-01T00:00:00+03:00 |
      | $5  |      | $news_lid | 2015-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ],
        "tabs_limits": [
          {"type": "news", "min_count": 3}
        ],
        "from_date": "2019-01-01T00:00:00+03:00",
        "chunk_size": 1
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab  |
      | $1  | news |
      | $2  | news |
      | $3  | news |
      | $4  |      |
      | $5  |      |


  Scenario: Task should work correctly on messages with same received date
    Given new empty user "Lony"
    And user has SO labels
      | lid       | so_type |
      | $news_lid | 100     |
    And user has messages in "inbox"
      | mid | tab | lids      | received_date             |
      | $1  |     | $news_lid | 2018-01-01T00:00:00+03:00 |
      | $2  |     |           | 2018-01-01T00:00:00+03:00 |
      | $3  |     |           | 2018-01-01T00:00:00+03:00 |
      | $4  |     |           | 2018-01-01T00:00:00+03:00 |
      | $5  |     |           | 2018-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ],
        "tabs_limits": [
          {"type": "news", "min_count": 10}
        ],
        "from_date": "2019-01-01T00:00:00+03:00",
        "chunk_size": 1
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  |          |
      | $3  |          |
      | $4  |          |
      | $5  |          |


  Scenario: Task should re-tab already tabbed messages if force
    Given new empty user "Selina"
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab      | lids        |
      | $1  | social   | $news_lid   |
      | $2  | news     |             |
      | $3  | relevant | $social_lid |
      | $4  |          | $social_lid |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"},
          {"type": "social", "so_type": "101"}
        ],
        "force": true
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  | relevant |
      | $3  | social   |
      | $4  | social   |


  Scenario: Task with force and date-count conditions should re-tab already tabbed messages until enough
    Given new empty user "Ivy"
    And user has SO labels
      | lid       | so_type |
      | $news_lid | 100     |
    And user has messages in "inbox"
      | mid | tab      | lids      | received_date             |
      | $1  | relevant | $news_lid | 2019-01-01T00:00:00+03:00 |
      | $2  |          | $news_lid | 2018-01-01T00:00:00+03:00 |
      | $3  | social   | $news_lid | 2017-01-01T00:00:00+03:00 |
      | $4  |          | $news_lid | 2016-01-01T00:00:00+03:00 |
      | $5  | social   | $news_lid | 2015-01-01T00:00:00+03:00 |
    When we plan "move_messages_to_tabs"
      """
      {
        "final": false,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ],
        "tabs_limits": [
          {"type": "news", "min_count": 3}
        ],
        "from_date": "2019-01-01T00:00:00+03:00",
        "chunk_size": 1,
        "force": true
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab    |
      | $1  | news   |
      | $2  | news   |
      | $3  | news   |
      | $4  |        |
      | $5  | social |

  Scenario: Task with skip_done should skip users with can_read_tabs true
    Given new empty user "Harleen"
    And user can read tabs
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
    And user has messages in "inbox"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
    When we plan "move_messages_to_tabs"
      """
      {
        "skip_done": true,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab      |
      | $1  |          |

  Scenario: Task with skip_full should skip users who is fully done
    Given new empty user "Vicky"
    And user can read tabs
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
    And user has messages in "inbox"
      | mid | tab    | lids        |
      | $1  | social | $news_lid   |
    When we plan "move_messages_to_tabs"
      """
      {
        "skip_full": true,
        "force": true,
        "mapping": [
          {"type": "news", "so_type": "100"}
        ]
      }
      """
    Then task is successful
    And user has messages in "inbox"
      | mid | tab    |
      | $1  | social |
