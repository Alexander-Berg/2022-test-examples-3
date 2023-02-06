Feature: Event json data operations

  Scenario: Organizer update event json data
    Given users
      | login    | type  |
      | org      | BASIC |
      | attendee | BASIC |
    And meeting
      | name | organizer | attendees |
      | meet | org       | attendee  |
    And service 'maya' with tvm id=2002194

    When 'org' set 'meet' event json data to '{"foo": "bar", "index": 42}' using 'maya' service
    Then meeting 'meet' should contain the following json data: '{"foo": "bar", "index": 42}'

  Scenario: Organizer update event json data using non-maya client
    Given users
      | login    | type  |
      | org      | BASIC |
      | attendee | BASIC |
    And meeting
      | name | organizer | attendees | data      |
      | meet | org       | attendee  | {"a": 42} |

    When 'org' set 'meet' event json data to '{"foo": "bar", "index": 42}'
    Then meeting 'meet' should contain the following json data: '{"a": 42}'

  Scenario: Organizer reset event json data
    Given users
      | login    | type  |
      | org      | BASIC |
      | attendee | BASIC |
    And meeting
      | name | organizer | attendees |
      | meet | org       | attendee  |
    And service 'maya' with tvm id=2002194

    When 'org' reset 'meet' event json data using 'maya' service
    Then meeting 'meet' should not contain json data

  Scenario: Attendee update event json data
  Organizer allow attendees can edit an event.
    Given users
      | login    | type  |
      | org      | BASIC |
      | attendee | BASIC |
    And meeting
      | name | organizer | attendees |
      | meet | org       | attendee  |
    And 'org' allow attendees can edit meeting 'meet'
    And service 'maya' with tvm id=2002194

    When 'attendee' set 'meet' event json data to '{"foo": "bar", "index": 42}' using 'maya' service
    Then meeting 'meet' should contain the following json data: '{"foo": "bar", "index": 42}'

  Scenario: Attendee update event json data
  Organizer don't allow attendees can edit an event.
    Given users
      | login    | type  |
      | org      | BASIC |
      | attendee | BASIC |
    And meeting
      | name | organizer | attendees |
      | meet | org       | attendee  |
    And service 'maya' with tvm id=2002194

    When 'attendee' set 'meet' event json data to '{"foo": "bar", "index": 42}' using 'maya' service
    Then meeting 'meet' should not contain json data
