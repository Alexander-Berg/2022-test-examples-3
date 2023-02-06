Feature: FromVcard
  Sheltie transform vcard to json

  Background:
    Given sheltie is started
    And sheltie response to ping

  Scenario: Transform from vcard
    When we request sheltie from_vcard
    Then response is "json_from_vcard"
