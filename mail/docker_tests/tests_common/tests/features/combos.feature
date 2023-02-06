Feature: Common scenarios

##### given
  Scenario: Combo in given
    Given var1 is qwerty and var2 is asdf
    Then var1 is qwerty
    And var2 is asdf

  Scenario: Combo in given with params
    Given var1 is "qwerty" and var2 is "asdf"
    Then var1 is qwerty
    And var2 is asdf

  Scenario: Multiline combo in given
    Given var1 is qwerty and var2 is asdf from text
    Then var1 is qwerty
    And var2 is asdf

##### when
  Scenario: Combo in when
    Given nothing
    When we set var1 to qwerty and var2 to asdf
    Then var1 is qwerty
    And var2 is asdf

  Scenario: Combo in when with params
    Given nothing
    When we set var1 to "qwerty" and var2 to "asdf"
    Then var1 is qwerty
    And var2 is asdf

  Scenario: Multiline combo in when
    Given nothing
    When we set var1 to qwerty and var2 to asdf from text
    Then var1 is qwerty
    And var2 is asdf

##### then
  Scenario: Combo in then
    Given nothing
    When we set var1 to qwerty
    And we set var2 to asdf
    Then var1 is qwerty and var2 is asdf

  Scenario: Combo in then with params
    Given nothing
    When we set var1 to qwerty
    And we set var2 to asdf
    Then var1 is "qwerty" and var2 is "asdf"

  Scenario: Multiline combo in then
    Given nothing
    When we set var1 to qwerty
    And we set var2 to asdf
    Then var1 is qwerty and var2 is asdf from text
