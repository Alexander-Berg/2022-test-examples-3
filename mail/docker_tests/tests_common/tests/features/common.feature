Feature: Common scenarios

  Scenario: Something in given
    Given var is qwerty
    Then var is qwerty

  Scenario: String steps
    Given nothing
    When we string set var to qwerty
    Then var is qwerty

  Scenario: Parametrized steps
    Given nothing
    When we parametrized set var to "qwerty"
    Then var is qwerty

  Scenario Outline: Outlined steps
    Given nothing
    When we outlined set var to <param>
    Then var is qwerty
  Examples:
    | param  |
    | qwerty |

  Scenario: Steps with text
    Given nothing
    When we set var from text
      """
        qwerty
      """
    Then stripped var is qwerty

  Scenario: Step text is reset after each step
    Given nothing
    When we set var from text
      """
        qwerty
      """
    Then context text is not set in this step

  Scenario: Steps with table
    Given nothing
    When we set var from table
      | var    |
      | qwerty |
    Then var is qwerty

  Scenario Outline: Steps with outlined table
    Given nothing
    When we set var from outlined table
      | var     |
      | <param> |
    Then var is qwerty
  Examples:
    | param  |
    | qwerty |
