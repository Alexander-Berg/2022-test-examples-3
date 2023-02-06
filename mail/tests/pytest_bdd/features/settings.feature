Feature: maildb settings

  Scenario: Creating new settings
    Given new initialized user
    When we create new setting "my_god" with value "is_diablo"
    Then user has setting "my_god" with value "is_diablo"

  Scenario: Deleting settings from user which have they
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    When we delete settings
    Then "delete" operation ended with result "1"
    And user settings has been deleted

  Scenario: Deleting settings from user which have't they
    Given new initialized user
    When we delete settings
    Then "delete" operation ended with result "0"

  Scenario: Update settings from user which have't they
    Given new initialized user
    When we update setting "my_god" with value "is_mephisto"
    Then "update" operation ended with result "0"

  Scenario: Updating exists setting
    Given new initialized user
    And new setting "my_god" with value "is_diablo"
    When we update setting "my_god" with value "is_mephisto"
    Then "update" operation ended with result "1"
    And user has setting "my_god" with value "is_mephisto"

  Scenario: Updating not exists setting
    Given new initialized user
    And new setting "my_god" with value "is_diablo"
    When we update setting "my_father" with value "is_mephisto"
    Then "update" operation ended with result "1"
    And user has setting "my_father" with value "is_mephisto"

  Scenario: Creating bulk modify settings task
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And empty bulk modify settings task
    When we create new bulk modify task with name "new_task" and type "update_for_all" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    And we have bulk task modify settings with name "new_task"
    And user has status "false" for "update_for_all"

  Scenario: Creating bulk modify settings task when already has bulk modify task
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And empty bulk modify settings task
    When we create new bulk modify task with name "old_task" and type "update_for_all" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    And we have bulk task modify settings with name "old_task"
    And user has status "false" for "update_for_all"
    When we create new bulk modify task with name "new_task" and type "update_for_all" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "false"

  Scenario: Creating bulk modify settings task when already has bulk modify task with different type
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And empty bulk modify settings task
    When we create new bulk modify task with name "old_task" and type "update_for_all" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    And we have bulk task modify settings with name "old_task"
    And user has status "false" for "update_for_all"
    When we create new bulk modify task with name "old_task" and type "init" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "false"

  Scenario: Bulk udate settings for all
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And empty bulk modify settings task
    When we create new bulk modify task with name "new_task" and type "update_for_all" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    When we bulk update setting 
    Then user has status "true" for "update_for_all"
    And user has setting "my_god" with value "is_diablo"

  Scenario: Bulk init settings 
    Given new initialized user
    And empty bulk modify settings task
    When we create new bulk modify task with name "new_task" and type "init" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    When we bulk init setting
    Then user has status "true" for "init"
    And user has only setting "my_god" with value "is_diablo"

  Scenario: Bulk udate settings by uids
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And empty bulk modify settings task
    When we create new bulk modify task with name "new_task" and type "update_by_uids" and setting "my_god" with value "is_diablo"
    Then creating bulk modification task ended with result "true"
    And we no have users for "update_by_uids"
    When we add user for update
    And we bulk update setting
    Then user has setting "my_god" with value "is_diablo"

  Scenario: Erase settings from user
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And add setting "my_name" with value "is_hellokitty"
    When we erase settings "my_god,my_name"
    Then "erase" operation ended with result "1"
    And user has not setting "my_god" with value "is_mephisto"
    And user has not setting "my_name" with value "is_hellokitty"


  Scenario: Erase settings from user which have't they
    Given new initialized user
    And new setting "my_god" with value "is_mephisto"
    And add setting "my_name" with value "is_hellokitty"
    When we erase settings "my_glory,my_name"
    Then "erase" operation ended with result "1"
    And user has setting "my_god" with value "is_mephisto"
    And user has not setting "my_name" with value "is_hellokitty"
