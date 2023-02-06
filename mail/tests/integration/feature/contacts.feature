Feature: Contacts
  Collie supports user contacts in db

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Create single contact
    When we prepare contact with emails
    And we request collie to create contacts
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Create multiple contacts
    When we request collie to create tag
    And we prepare contacts with one contact tagged
    And we request collie to create contacts
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Create multiple contacts and check tagged emails
    When we request collie to create tag
    And we prepare contacts with one contact tagged and same emails
    And we request collie to create contacts
    And we request collie to get emails with tag ids
    Then response is ok
    And response has emails with tag

  Scenario: Get contacts response satisfies json schema
    When we prepare contact with emails
    And we request collie to create contacts
    And we request collie to get last created contacts
    Then response is ok
    And response is verified by json schema "existing_contacts.json"

  Scenario: Get contacts response with contact tags
    When we request collie to create tag
    And we prepare contacts with one contact tagged
    And we request collie to create contacts
    And we request collie to get last created contacts
    Then response is ok
    And response has created tag id

  Scenario: Get contacts response with emails
    When we prepare contacts
    And we request collie to create contacts
    And we request collie to get last created contacts
    Then response is ok
    And response has contacts with vcards with created emails
    And response has contacts with created emails

  Scenario: Get contacts response with emails with offset and limit
    When we prepare contacts
    And we request collie to create contacts
    And we request collie to get last created contacts with offset and limit
    Then response is ok
    And response has contact with vcard with created emails
    And response has contact with created emails

  Scenario: Get all contacts
    When we prepare contacts
    And we request collie to create contacts
    And we request collie to get all contacts
    Then response is ok
    And response has contacts with vcards with created emails

  Scenario: Remove contacts
    When we request collie to create tag
    And we prepare contacts with one contact tagged
    And we request collie to create contacts
    And we request collie to remove last created contacts
    Then response is ok
    And response is verified by json schema "revision.json"

  Scenario: Get contacts count
    When we prepare contact with emails
    And we request collie to create contacts
    And we request collie to get contacts count
    Then response is ok
    And response is verified by json schema "contacts_count.json"

  Scenario: Create multiple contacts as emails
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Don't create multiple contacts as emails when collect_addresses disabled
    When we expect get profile request to settings with collect_addresses "off"
    And we request collie to add emails
    Then response is ok
    And response is verified by json schema "created_contacts.json"
    And response has empty created contacts

  Scenario: Create multiple contacts as emails and don't create duplicated
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails
    And we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Create multiple contacts as emails and check the result
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails
    And we request collie to get last created contacts
    Then response is ok
    And response has created contacts as emails

  Scenario: Create multiple contacts as emails passed via query string
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails via "query string"
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Create multiple contacts as emails passed via query string and check the result
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails via "query string"
    And we request collie to get last created contacts
    Then response is ok
    And response has created contacts as emails

  Scenario: Create multiple contacts as emails passed via request body
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails via "request body"
    Then response is ok
    And response is verified by json schema "created_contacts.json"

  Scenario: Create multiple contacts as emails passed via request body and check the result
    When we expect get profile request to settings with collect_addresses "on"
    And we request collie to add emails via "request body"
    And we request collie to get last created contacts
    Then response is ok
    And response has created contacts as emails

  Scenario: Get contacts via searchContacts
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to search contacts
    Then response is ok
    And response is verified by json schema "search_contacts_ungrouped_result.json"

  Scenario: Get contacts via searchContacts and check the result
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we add directory_entries to created contacts
    And we request collie to create contacts
    And we request collie to search contacts
    Then response is ok
    And response has contact emails as ungrouped result

  Scenario: Update contact
    When we prepare contact with emails
    And we request collie to create contacts
    And we request collie to create tag
    And we request collie to update and tag last created contact
    Then response is ok
    And response is verified by json schema "revision.json"

  Scenario: Update contact and check tagged entities
    When we prepare contact with emails
    And we request collie to create contacts
    And we request collie to create tag
    And we request collie to update and tag last created contact
    And we request collie to get contacts with tag
    Then response is ok
    And response has updated contact with tag

  Scenario: Update contact and check there are no tagged emails
    When we request collie to create tag
    And we prepare tagged contact with emails
    And we request collie to create contacts
    And we request collie to update and untag last created contact
    And we request collie to get emails with tag ids
    Then response is ok
    And response has no tagged emails

  Scenario: Update contact and check there are no tagged contacts
    When we request collie to create tag
    And we prepare tagged contact with emails
    And we request collie to create contacts
    And we request collie to update and untag last created contact
    And we request collie to get contacts with tag
    Then response is ok
    And response has no tagged contacts

  Scenario: Update contact with tagged emails
    When we request collie to create tag
    And we prepare contacts with multiple contacts tagged
    And we request collie to create contacts
    And we request collie to update contact with tagged emails
    Then response is ok
    And response is verified by json schema "revision.json"
