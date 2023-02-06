Feature: ToVcard
  Sheltie transform json to vcard

  Background:
    Given sheltie is started
    And sheltie response to ping

  Scenario: Transform to vcard
    When we request sheltie to_vcard
    Then response is "vcard_from_json"
