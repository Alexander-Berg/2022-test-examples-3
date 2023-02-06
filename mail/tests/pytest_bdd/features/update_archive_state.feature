Feature: code.exchange_archive_state

  Scenario: exchange_archive_state user is not here
    Given new initialized user
    And archive is in "archivation_complete" state
    When we transfer him to another shard
    And we update archive state from "archivation_complete" to "cleaning_requested" assuming user in "archived" state
    Then archive update result is "user_not_here"
    And archive is in "archivation_complete" state
    And user has "active" state
    And he is not here


  Scenario: exchange_archive_state user is in wrong state
    Given new initialized user
    And user has "archived" state
    And archive is in "archivation_complete" state
    When we update archive state from "archivation_complete" to "cleaning_requested" assuming user in "<u_state>" state
    Then archive update result is "user_wrong_state"
    And archive is in "archivation_complete" state
    And he is here
    And user has "archived" state

    Examples:
      | u_state  |
      | active   |
      | inactive |
      | notified |
      | frozen   |
      | deleted  |
      | special  |


  Scenario: exchange_archive_state user without archive
    Given new initialized user
    And user has "<u_state>" state
    When we update archive state from "archivation_complete" to "cleaning_requested" assuming user in "<u_state>" state
    Then archive update result is "archive_not_found"
    And user has "<u_state>" state
    And he is here

    Examples:
      | u_state  |
      | active   |
      | inactive |
      | notified |
      | frozen   |
      | archived |
      | deleted  |
      | special  |

  Scenario: exchange_archive_state wrong archive transitions
    Given new initialized user
    And user has "<u_state>" state
    And archive is in "<current>" state
    When we update archive state from "<from>" to "<to>" assuming user in "<u_state>" state
    Then archive update result is "archive_not_found"
    And archive is in "<current>" state
    And user has "<u_state>" state
    And he is here

    Examples:
      | current                 | from                    | to                      | u_state  |
      | cleaning_requested      | archivation_complete    | restoration_requested   | active   |
      | restoration_requested   | archivation_complete    | cleaning_requested      | inactive |
      | archivation_in_progress | archivation_complete    | archivation_error       | notified |
      | restoration_in_progress | restoration_error       | restoration_complete    | frozen   |
      | cleaning_requested      | restoration_error       | archivation_in_progress | special  |


  Scenario: exchange_archive_state correct archive transitions
    Given new initialized user
    And user has "<u_state>" state
    And archive is in "<current>" state
    And archive has "not empty notice" notice

    When we update archive state with "<current>-<from>-<to>-<u_state>" from "<from>" to "<to>" assuming user in "<u_state>" state

    Then archive update result is ok
    And archive is in "<to>" state
    And archive has "<current>-<from>-<to>-<u_state>" notice

    And user has "<u_state>" state
    And he is here

    Examples:
      | current                 | from                    | to                      | u_state  |
      | archivation_complete    | archivation_complete    | cleaning_requested      | archived |
      | cleaning_requested      | cleaning_requested      | cleaning_requested      | inactive |
      | cleaning_requested      | archivation_complete    | cleaning_requested      | notified |
      | archivation_complete    | archivation_complete    | restoration_requested   | frozen   |
      | restoration_requested   | restoration_requested   | restoration_requested   | archived |
      | restoration_requested   | archivation_complete    | restoration_requested   | deleted  |


  Scenario: exchange_archive_state notice remains the same in the case of an error
    Given new initialized user
    And user has "active" state
    And archive is in "restoration_in_progress" state
    And archive has "not empty notice" notice

    When we update archive state with "<notice_text>" from "<from_state>" to "restoration_error" assuming user in "<u_state>" state

    Then archive update result is "<error>"
    And archive is in "restoration_in_progress" state
    And archive has "not empty notice" notice

    And user has "active" state
    And he is here

    Examples:
      | u_state  | from_state           | notice_text | error             |
      | frozen   | archivation_complete | 123456      | user_wrong_state  |
      | active   | archivation_complete | 654321      | archive_not_found |
