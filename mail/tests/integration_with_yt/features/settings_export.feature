Feature: settings_export via shiva

  Background: Should not have shiva tasks or yt yables but should have fields file
    Given there are no shiva tasks
    And there are no active users in shard
    And there are no tables in yt for settings
    But there are file with public fields for settings
      """
        uid
        known_field
      """
    And there are file with private fields for settings
      """
        private_field
      """

  Scenario: Should create table with known fields in schema
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are empty shard table in yt for settings with schema
      | name          | type   |
      | uid           | int64  |
      | from_shard_id | int64  |
      | signs_count   | int64  |
      | known_field   | string |
      | private_field | string |

  Scenario: Should dump settings from known fields
    Given new user "Micha"
    And "Micha" has setting "known_field" with value "on"
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for settings with 1 rows
      | uid   | known_field | private_field |
      | Micha | on          | null          |

  Scenario: Should hide settings from private fields
    Given new user "Herbert"
    And "Herbert" has setting "private_field" with value "42"
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for settings with 1 rows
      | uid     | private_field | known_field |
      | Herbert | !42           | null        |

  Scenario: Should hide settings from unknown fields
    Given new user "Gotthilf"
    And "Gotthilf" has setting "unknown_field" with value "off"
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for settings with 1 rows
      | uid      | unknown_field | known_field | private_field |
      | Gotthilf | !off          | null        | null          |

  Scenario: Should dump signs count
    Given new user "Gilroy"
    And "Gilroy" has 3 signs in settings
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for settings with 1 rows
      | uid    | signs_count | known_field | private_field |
      | Gilroy | 3           | null        | null          |

  Scenario: Should ignore dumped settings
    Given new user "Oswine"
    And "Oswine" has setting "known_field" with value "asdf"
    And there are shard table in yt for settings with row for "Oswine"
    And "Oswine" updates setting "known_field" with value "on"
    When we make settings_export request
    Then shiva responds ok
    And all shiva tasks finished
    And there are shard table in yt for settings with 1 rows
      | uid     | known_field |
      | Oswine  | asdf        |
