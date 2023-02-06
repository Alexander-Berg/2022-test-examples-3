Feature: Organizer abilities

  Scenario: Organizer invites a new attendee
    Given users
      | login        | type  |
      | org          | BASIC |
      | attendee     | BASIC |
      | new_attendee | BASIC |
    And meeting
      | name | organizer | attendees |
      | meet | org       | attendee  |

    When 'org' invites 'new_attendee' to 'meet'

    Then meeting 'meet' should contain the following attendees: ["attendee", "new_attendee"]
