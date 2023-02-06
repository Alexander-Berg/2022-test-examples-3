Feature: Testing transfer with change_tab set tab for old messages
  Scenario: Transfer should create tabs
    Given new user in first shard
    And user does not have tabs
    When we transfer his metadata to second shard with
      """
      tabs_mapping:
        - so_type: '101'
          type: social
        - so_type: '100'
          type: news
      """
    Then user has tabs "relevant, news, social"


  Scenario: Transfer should set can_read_tabs true
    Given new user in first shard
    When we transfer his metadata to second shard with
      """
      tabs_mapping:
        - so_type: '101'
          type: social
        - so_type: '100'
          type: news
      """
    Then user can read tabs


  Scenario: Transfer should fill empty tabs for messages in inbox
    Given new empty user in first shard
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
      | $2  |     |             |
      | $3  |     | $social_lid |
    When we transfer his metadata to second shard with
      """
      tabs_mapping:
        - so_type: '101'
          type: social
        - so_type: '100'
          type: news
      """
    Then user has messages in "inbox"
      | mid | tab      |
      | $1  | news     |
      | $2  | relevant |
      | $3  | social   |


  Scenario: Transfer should ignore messages not in inbox
    Given new empty user in first shard
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "sent"
      | mid | tab | lids        |
      | $1  |     | $news_lid   |
      | $2  |     |             |
      | $3  |     | $social_lid |
    When we transfer his metadata to second shard with
      """
      tabs_mapping:
        - so_type: '101'
          type: social
        - so_type: '100'
          type: news
      """
    Then user has messages in "sent"
      | mid | tab |
      | $1  |     |
      | $2  |     |
      | $3  |     |


  Scenario: Transfer should ignore already tabbed messages
    Given new empty user in first shard
    And user has SO labels
      | lid         | so_type |
      | $news_lid   | 100     |
      | $social_lid | 101     |
    And user has messages in "inbox"
      | mid | tab      | lids        |
      | $1  | social   | $news_lid   |
      | $2  | news     |             |
      | $3  | relevant | $social_lid |
    When we transfer his metadata to second shard with
      """
      tabs_mapping:
        - so_type: '101'
          type: social
        - so_type: '100'
          type: news
      """
    Then user has messages in "inbox"
      | mid | tab      |
      | $1  | social   |
      | $2  | news     |
      | $3  | relevant |
