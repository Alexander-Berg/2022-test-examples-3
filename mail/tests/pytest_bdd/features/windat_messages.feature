Feature: Store windat messages

  Scenario: Store windat message with different hids
    Given new initialized user
    When we store "$1" into "inbox"
    And we try add "$1" "100.500" "1.2" "1.1:xx" to windat as "try_add_windat"
    Then commit "try_add_windat" should produce "HidMisMatch"


  Scenario: Store windat message with wrong st_id
    Given new initialized user
    When we store "$1" into "inbox"
    And we add windat attachment to "$1" with "100.500" st_id as "try_add_windat"
    Then commit "try_add_windat" should produce "StidMismatch"
