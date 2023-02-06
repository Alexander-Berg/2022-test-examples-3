Feature: Ping
  Collie answer to ping

  Background:
    Given collie is started

  Scenario: Ping pong
    When we ping collie
    Then response is "pong"
