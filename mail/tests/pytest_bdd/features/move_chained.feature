Feature: Move messages

  Background: All on new initialized user with 7 message in inbox
    Given new initialized user
    When we store "$[1:7]" into "inbox"

  Scenario: Move messages sanity check
    Then "inbox" has "7" messages
    And chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move all to trash
    When we move "$[1:7]" to "trash"
    Then "inbox" has not messages
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $2  | 2       | 1          | 2        |
      | $3  | 3       | 1          | 3        |
      | $4  | 4       | 4          | 1        |
      | $5  | 5       | 4          | 2        |
      | $6  | 6       | 4          | 3        |
      | $7  | 7       | 7          | 1        |
    And "trash" has "7" messages
    And chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: prez@ case for chained
    When we store "$[8:10]" into "inbox"
    Then chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 8 9 | 10 |
    When we move "$[1:9]" to "trash"
    Then chained "inbox" is
      | 10 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $2  | 2       | 1          | 2        |
      | $3  | 3       | 1          | 3        |
      | $4  | 4       | 4          | 1        |
      | $5  | 5       | 4          | 2        |
      | $6  | 6       | 4          | 3        |
      | $7  | 7       | 7          | 1        |
      | $8  | 8       | 7          | 2        |
      | $9  | 9       | 7          | 3        |
      | $10 | 0       | 10         | 1        |

  Scenario: move from interval boundaries
    When we move "$1, $7" to "trash"
    Then chained "inbox" is
      | 2 3 | 4 5 6 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $7  | 7       | 7          | 1        |
      | $2  | 0       | 2          | 2        |
    And chained "trash" is
      | 1 2 |
    When we move "$2, $6" to "trash"
    Then chained "inbox" is
      | 3 | 4 5 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $2  | 2       | 2          | 1        |
      | $6  | 6       | 4          | 3        |
      | $3  | 0       | 3          | 1        |
    And chained "trash" is
      | 1 2 3 | 4 |
    When we move "$3, $5" to "trash"
    Then chained "inbox" is
      | 4 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $3  | 3       | 3          | 1        |
      | $5  | 5       | 4          | 2        |
    And chained "trash" is
      | 1 2 3 | 4 5 6 |
    When we move "$4" to "trash"
    Then "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $4  | 4       | 4          | 1        |
    And chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move always chained
    When we move "$1, $4, $7" to "sent"
    Then chained "inbox" is
      | 2 3 5 6 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $4  | 4       | 4          | 1        |
      | $7  | 7       | 7          | 1        |
      | $2  | 0       | 2          | 4        |
    And "sent" has "3" messages
    And chained "sent" is
      | 1 2 3 |
    When we move "$2" to "sent"
    Then chained "inbox" is
      | 3 5 6 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $2  | 2       | 2          | 1        |
      | $3  | 0       | 3          | 3        |
    And chained "sent" is
      | 1 2 3 | 4 |
    When we move "$3, $5" to "sent"
    Then chained "inbox" is
      | 6 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $3  | 3       | 3          | 1        |
      | $5  | 5       | 3          | 2        |
      | $6  | 0       | 6          | 1        |
    And chained "sent" is
      | 1 2 3 | 4 5 6 |
    When we move "$6" to "sent"
    Then "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $6  | 6       | 6          | 1        |
    And chained "sent" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move unchained first
    When we move "$2, $3, $5, $6" to "sent"
    Then chained "inbox" is
      | 1 | 4 | 7 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $2  | 2       | 1          | 2        |
      | $3  | 3       | 1          | 3        |
      | $5  | 5       | 4          | 2        |
      | $6  | 6       | 4          | 3        |
    And chained "sent" is
      | 1 2 3 | 4 |
    When we move "$1, $4, $7" to "sent"
    Then "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $4  | 4       | 4          | 1        |
      | $7  | 7       | 7          | 1        |
    And chained "sent" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move chained and unchained
    When we move "$1, $2, $4, $5" to "trash"
    Then chained "inbox" is
      | 3 6 | 7 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $2  | 2       | 1          | 2        |
      | $4  | 4       | 4          | 1        |
      | $5  | 5       | 4          | 2        |
      | $3  | 0       | 3          | 2        |
    And chained "trash" is
      | 1 2 3 | 4 |
    When we move "$6" to "trash"
    Then chained "inbox" is
      | 3 | 7 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $6  | 6       | 3          | 2        |
    And chained "trash" is
      | 1 2 3 | 4 5 |
    When we move "$3, $7" to "trash"
    Then "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $3  | 3       | 3          | 1        |
      | $7  | 7       | 7          | 1        |
    And chained "trash" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move fill old and new chains
    When we move "$1, $5, $6, $7" to "drafts"
    Then "inbox" has "3" messages
    And chained "inbox" is
      | 2 3 | 4 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $1  | 1       | 1          | 1        |
      | $5  | 5       | 4          | 2        |
      | $6  | 6       | 4          | 3        |
      | $7  | 7       | 7          | 1        |
      | $2  | 0       | 2          | 2        |
    And "drafts" has "4" messages
    And chained "drafts" is
      | 1 2 3 | 4 |
    When we move "$2" to "drafts"
    Then "inbox" has "2" messages
    And chained "inbox" is
      | 3 | 4 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $2  | 2       | 2          | 1        |
      | $3  | 0       | 3          | 1        |
    And "drafts" has "5" messages
    And chained "drafts" is
      | 1 2 3 | 4 5 |
    When we move "$3, $4" to "drafts"
    Then "inbox" has not messages
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $3  | 3       | 3          | 1        |
      | $4  | 4       | 4          | 1        |
    And "drafts" has "7" messages
    And chained "drafts" is
      | 1 2 3 | 4 5 6 | 7 |

  Scenario: move from tail
    When we move "$7" to "trash"
    Then chained "inbox" is
      | 1 2 3 | 4 5 6 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $7  | 7       | 7          | 1        |
    And chained "trash" is
      | 1 |
    When we move "$4, $5" to "trash"
    Then chained "inbox" is
      | 1 2 3 6 |
  # should ask imap guys how we should treat chained union?
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $4  | 4       | 4          | 1        |
      | $5  | 5       | 4          | 2        |
      | $1  | 0       | 1          | 4        |
    And chained "trash" is
      | 1 2 3 |
    When we move "$2, $3, $6" to "trash"
    Then chained "inbox" is
      | 1 |
    And "inbox" chained log is
      | mid | imap_id | chained_id | distance |
      | $2  | 2       | 1          | 2        |
      | $3  | 3       | 1          | 3        |
      | $6  | 6       | 1          | 4        |
    And chained "trash" is
      | 1 2 3 | 4 5 6 |

  Scenario: Move between inbox and trash
    When we move "$[1:5]" to "trash"
    Then chained "inbox" is
      | 6 | 7 |
    And chained "trash" is
      | 1 2 3 | 4 5 |
    When we move "$1, $2" to "inbox"
    Then chained "trash" is
      | 3 | 4 5 |
    And chained "inbox" is
      | 6 | 7 8 9 |
    And in "inbox" there are "4" messages
      | mid | imap_id |
      | $6  | 6       |
      | $7  | 7       |
      | $1  | 8       |
      | $2  | 9       |
    When we move "$4, $5" to "inbox"
    Then chained "trash" is
      | 3 |
    And chained "inbox" is
      | 6 | 7 8 9 | 10 11 |
    And in "inbox" there are "6" messages
      | mid | imap_id |
      | $6  | 6       |
      | $7  | 7       |
      | $1  | 8       |
      | $2  | 9       |
      | $4  | 10      |
      | $5  | 11      |
    And in "trash" there is one message
      | mid | imap_id |
      | $3  | 3       |
    When we move "$[4:7]" to "trash"
    Then chained "inbox" is
      | 8 9 |
    Then chained "trash" is
      | 3 6 7 | 8 9 |
