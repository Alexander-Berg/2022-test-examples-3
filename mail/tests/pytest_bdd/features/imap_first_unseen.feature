Feature: Special cases for IMAP first unseen

  Scenario: Delete seen before first_unseen, and unseen after first_unseen
    Given new initialized user
    When we store into "inbox"
      | mid | flags |
      | $1  | seen  |
      | $2  | seen  |
      | $3  |       |
      | $4  |       |
      | $5  |       |
    Then "inbox" has first_unseen at "3", first_unseen_id at "3"
    When we delete "$1, $4"
    Then "inbox" has first_unseen at "2", first_unseen_id at "3"
