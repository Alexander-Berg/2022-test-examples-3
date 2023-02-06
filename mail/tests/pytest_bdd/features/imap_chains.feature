Feature: Special cases for mail.box.chain

  Background: All on new initialized user with 9 message in inbox
    Given new initialized user
    When we store "$[1:9]" into "inbox"
    Then chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 8 9 |

  Scenario: [MAILPG-522] box.chain greater then default chain size
    When we move "$4, $7" to "drafts"
    Then chained "inbox" is
      | 1 2 3 5 6 8 9 |
    When we move "$4, $7" to "inbox"
    Then message "$4" in "inbox" with "10" imap_id
    And message "$7" in "inbox" with "11" imap_id
    And chained "inbox" is
      | 1 2 3 5 6 8 9 | 10 11 |

  Scenario: changelog entry for imap_id regeneration
    When we generate new imap_id for message "$5"
    Then we have last imap_id result equals "10"
    And in changelog there are reindex changes
      | revision | mids |
      | 11       | $5   |

  Scenario: new imap_id for unchained letter not in last chain
    When we generate new imap_id for message "$5"
    Then we have last imap_id result equals "10"
    And message "$5" in "inbox" with "10" imap_id
    And chained "inbox" is
      | 1 2 3 | 4 6 | 7 8 9 | 10 |
    When we generate new imap_id for message "$3"
    Then we have last imap_id result equals "11"
    And message "$3" in "inbox" with "11" imap_id
    And chained "inbox" is
      | 1 2 | 4 6 | 7 8 9 | 10 11 |

  Scenario: new imap_id for unchained letter in last chain(with new chain)
    When we generate new imap_id for message "$9"
    Then we have last imap_id result equals "10"
    And message "$9" in "inbox" with "10" imap_id
    And chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 8 | 10 |

  Scenario: new imap_id for unchained letter in last chain(without new chain)
    When we delete "$9"
    And we generate new imap_id for message "$8"
    Then we have last imap_id result equals "10"
    And message "$8" in "inbox" with "10" imap_id
    And chained "inbox" is
      | 1 2 3 | 4 5 6 | 7 10 |

  Scenario: new imap_id for chained letter not in last chain
    When we generate new imap_id for message "$4"
    Then we have last imap_id result equals "10"
    And message "$4" in "inbox" with "10" imap_id
    And chained "inbox" is
      | 1 2 3 5 6 | 7 8 9 | 10 |
    When we generate new imap_id for message "$1"
    Then we have last imap_id result equals "11"
    And message "$1" in "inbox" with "11" imap_id
    And chained "inbox" is
      | 2 3 5 6 | 7 8 9 | 10 11 |

  Scenario: new imap_id for chained letter in last chain
    When we delete "$9"
    And we generate new imap_id for message "$7"
    Then we have last imap_id result equals "10"
    And message "$7" in "inbox" with "10" imap_id
    And chained "inbox" is
      | 1 2 3 | 4 5 6 8 | 10 |

  Scenario: set recent flag for regenerated message
    When we set "-recent" on "$2"
    Then "inbox" has "8" recent
    When we generate new imap_id for message "$2"
    Then we have last imap_id result equals "10"
    And "inbox" has "9" recent
    And we have last imap_id result equals "10"
    And chained "inbox" is
      | 1 3 | 4 5 6 | 7 8 9 | 10 |