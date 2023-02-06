Feature: Test hound returns correct nearest_messages

  Scenario Outline: test nearest messages for different dates
    Given test user with "3" messages
    And messages have received dates
      | mid | received_date |
      | $1  | <date_1>      |
      | $2  | <date_2>      |
      | $3  | <date_3>      |
    When we request nearest messages for mid "$2"
    Then response is OK
    And response has "<result>" mids in order

    Examples:
      | date_1 | date_2 | date_3 | result     |
      | new    | mid    | old    | $1, $2, $3 |
      | old    | mid    | new    | $3, $2, $1 |
      | mid    | new    | old    | $2, $1     |
      | new    | old    | mid    | $3, $2     |
      | mid    | mid    | old    | $1, $2, $3 |
      | mid    | mid    | new    | $1, $2     |
      | new    | mid    | mid    | $1, $2, $3 |
      | old    | mid    | mid    | $2, $3     |
      | mid    | mid    | mid    | $1, $2, $3 |
