Feature: Mail collectors

  Background: All on new users pair
    Given new initialized user "First"
    When we initialize new user "Second"

  Scenario: Create new collector
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    Then in changelog there is
      | revision | type             |
      | 2        | collector-create |
    And users collectors count is "1"
    And in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: null
      label_id: null
      """

  Scenario: Create 2 collectors
    When we initialize new user "Third"
    And we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    And we create collector from "Second"
      """
      auth_token: "iamouathtoken"
      """
    Then in changelog there is
      | revision | type             |
      | 2        | collector-create |
      | 3        | collector-create |
    And users collectors count is "2"

  Scenario: Create collector to folder
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      root_folder_id: 10
      """
    Then in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: 10
      label_id: null
      """

  Scenario: Create collector with label
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      label_id: 20
      """
    Then in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: null
      label_id: 20
      """

  Scenario: Create duplicated collector
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    And we try create collector from "First" as "$duplicated-collector"
      """
      auth_token: "iamouathtoken"
      """
    Then in changelog there is
      | revision | type             |
      | 2        | collector-create |
    And users collectors count is "1"
    And commit "$duplicated-collector" should produce "DuplicatedCollector"

  Scenario: Collector from myself
    When we try create collector from "Second" as "$myself-collector"
      """
      auth_token: "iamouathtoken"
      """
    Then commit "$myself-collector" should produce "CollectorFromMyself"

  Scenario: Delete collector
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    And we delete last created collector
    Then in changelog there is
      | revision | type             |
      | 2        | collector-create |
      | 3        | collector-delete |
    And users collectors count is "0"

  Scenario: Update metadata
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    Then in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: null
      label_id: null
      """
    When we update metadata for last created collector
      """
      root_folder_id: 17
      label_id: 6
      """
    Then in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: 17
      label_id: 6
      """
    And in changelog there is
      | revision | type             |
      | 2        | collector-create |
      | 3        | collector-update |

  Scenario: Update meta without changelog
    When we create collector from "First"
      """
      auth_token: "iamouathtoken"
      """
    Then in metadata for last created collector
      """
      auth_token: "iamouathtoken"
      root_folder_id: null
      label_id: null
      """
    When we silently update metadata for last created collector
      """
      auth_token: "newtoken"
      """
    Then in metadata for last created collector
      """
      auth_token: "newtoken"
      root_folder_id: null
      label_id: null
      """
    When we delete last created collector
    Then in changelog there is
      | revision | type             |
      | 2        | collector-create |
      | 3        | collector-delete |
