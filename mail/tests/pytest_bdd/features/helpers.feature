Feature: Different helpers tests

  Scenario: Check code.get_folder_path
    When we initialize new user "Alice"
    And she create "user" folder "life" as "$life"
    Then "$life.fid" folder path is "life"
    When we initialize new user "Bob"
    And he create "user" folder "do_not" as "$do_not"
    Then "$do_not.fid" folder path is "do_not"
    When "Alice" comeback
    And she create "user" folder "is" under "$life.fid" as "$is"
    Then "$is.fid" folder path is "life|is"
    When "Bob" comeback
    And he create "user" folder "trust" under "$do_not.fid" as "$trust"
    Then "$trust.fid" folder path is "do_not|trust"
    When "Alice" comeback
    And she create "user" folder "pain" under "$is.fid" as "$pain"
    Then "$pain.fid" folder path is "life|is|pain"
    When "Bob" comeback
    And he create "user" folder "anyone" under "$trust.fid" as "$anyone"
    Then "$anyone.fid" folder path is "do_not|trust|anyone"
