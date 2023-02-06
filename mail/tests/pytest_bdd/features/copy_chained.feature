Feature: Copy messages

  Background: All on new initialized user with 7 message in inbox
    Given new initialized user
    When we store "$[1:7]" into "inbox"

  Scenario: Copy chained basic case
    Then "inbox" has "7" messages
    And chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy all to trash
    When we copy "$[1:7]" to "trash"
    Then "inbox" has "7" messages
    And "trash" has "7" messages
    And chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy from interval boundaries
    When we copy "$1, $7" to "trash"
    Then chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 |
    And chained "trash" is
      | 1 2 |
    When we copy "$2, $6" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 |
    When we copy "$3, $5" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 5 6 |
    When we copy "$4" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy always chained
    When we copy "$1, $4, $7" to "sent"
    Then "sent" has "3" messages
    And chained "sent" is
      | 1 2 3 |
    When we copy "$2" to "sent"
    Then chained "sent" is
      | 1 2 3 | 4 |
    When we copy "$3, $5" to "sent"
    Then chained "sent" is
      | 1 2 3 | 4 5 6 |
    When we copy "$6" to "sent"
    Then chained "sent" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy unchained first
    When we copy "$2, $3, $5, $6" to "sent"
    Then chained "sent" is
      | 1 2 3 | 4 |
    When we copy "$1, $4, $7" to "sent"
    Then chained "sent" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy chained and unchained
    When we copy "$1, $2, $4, $5" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 |
    When we copy "$6" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 5 |
    When we copy "$3, $7" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy fill old and new chains
    When we copy "$1, $5, $6, $7" to "drafts"
    Then "drafts" has "4" messages
    And chained "drafts" is
      | 1 2 3 | 4 |
    When we copy "$2" to "drafts"
    Then "drafts" has "5" messages
    And chained "drafts" is
      | 1 2 3 | 4 5 |
    When we copy "$3, $4" to "drafts"
    Then "drafts" has "7" messages
    And chained "drafts" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: Copy from tail
    When we copy "$7" to "trash"
    Then chained "trash" is
      | 1 |
    When we copy "$4, $5" to "trash"
    Then chained "trash" is
      | 1 2 3 |
    When we copy "$2, $3, $6" to "trash"
    Then chained "trash" is
      | 1 2 3 | 4 5 6 |
