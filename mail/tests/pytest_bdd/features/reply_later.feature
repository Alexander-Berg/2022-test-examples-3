Feature: Reply later stickers

  Scenario: Should create sticker
    Given new initialized user with "$1" in "inbox"
    When we put a sticker on "$1"
    Then there is a sticker on "$1"

  Scenario: Should delete sticker
    Given new initialized user with "$1" in "inbox"
    When we put a sticker on "$1"
    And there is a sticker on "$1"
    Then we remove sticker from "$1"
    And there are no stickers on "$1"

  Scenario: Should not create too many stickers
    Given new initialized user with "$[1:51]" in "inbox"
    When we put a sticker on "$[1:50]"
    And we put a sticker on "$51" and catch an error
    Then there is a sticker on "$[1:50]"
    And there are no stickers on "$51"

  Scenario: Should remove incorrect stickers
    Given new initialized user with a stickered message in "<folder>"
    When we set "<started>" on "$1"
    And we set "<finished>" on "$1"
    And we remove incorrect stickers
    Then there are no stickers on "$1"
    Examples:
      | folder      | started                      | finished                     |
      | inbox       | -system:reply_later_started  | -system:reply_later_finished |
      | inbox       | +system:reply_later_started  | -system:reply_later_finished |
      | reply_later | -system:reply_later_started  | -system:reply_later_finished |
      | reply_later | -system:reply_later_started  | +system:reply_later_finished |

  Scenario: Should not remove correct stickers
    Given new initialized user with a stickered message in "<folder>"
    When we set "<started>" on "$1"
    And we set "<finished>" on "$1"
    And we remove incorrect stickers
    Then there is a sticker on "$1"
    Examples:
      | folder      | started                      | finished                     |
      | inbox       | +system:reply_later_started  | +system:reply_later_finished |
      | inbox       | -system:reply_later_started  | +system:reply_later_finished |
      | reply_later | +system:reply_later_started  | -system:reply_later_finished |
      | reply_later | +system:reply_later_started  | +system:reply_later_finished |
