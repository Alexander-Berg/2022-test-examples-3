Feature: ping
    Cloud sharpei answer for ping

    Scenario: Ping
    Given cloud sharpei is started
    When we /ping sharpei
    Then response status code is "200"
    And response body is "pong"