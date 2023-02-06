Feature: Tags
  Collie supports user tags for contacts in db

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Create tag
    When we request collie to create tag
    Then response is ok
    And response is verified by json schema "created_tag.json"

  Scenario: Get tags response satisfies json schema
    When we request collie to get tags
    Then response is ok
    And response is verified by json schema "existing_tags.json"

  Scenario: Get tags response with contacts count
    When we prepare contacts
    And we request collie to create contacts
    And we create "user" tag "named_tag" for last created contact
    And we request collie to get tags
    Then response is ok
    And response has created tag with contacts count "2"

  Scenario: Remove tag
    When we request collie to create tag
    And we prepare contacts with one contact tagged
    And we request collie to create contacts
    And we request collie to remove last created tag
    Then response is ok
    And response is verified by json schema "revision.json"

  Scenario: Get contacts with tag
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to get contacts with tag
    Then response is ok
    And response is verified by json schema "existing_contacts.json"

  Scenario: Get contacts with tag and check the result
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to get contacts with tag
    Then response is ok
    And response has contacts with tag with vcards with created emails
    And response has contacts with tag with created emails

  Scenario: Get contacts with tag with offset and limit
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to get contacts with tag with offset "1" and limit "1"
    Then response is ok
    And response is verified by json schema "existing_contacts.json"

  Scenario: Get contacts with tag with offset and limit and check the result
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to get contacts with tag with offset "1" and limit "1"
    Then response is ok
    And response has contact with tag with vcard with created emails
    And response has contact with tag with created emails

  Scenario: Update tag
    When we request collie to create tag
    And we request collie to update last created tag name to "updated_tag"
    Then response is ok
    And response is verified by json schema "revision.json"
