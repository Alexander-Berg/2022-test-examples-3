Feature: Carddav
  Collie supports Carddav requests

  Background:
    Given collie is started
    And collie response to ping
    And new passport user
    And TVM2 service tickets for collie
    And TVM2 user ticket
    And new contacts passport user

  Scenario: Delete contact if user has Phone contacts
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we request collie to delete "YA-1" contact
    Then response is ok

  Scenario: Delete contact if the user does not have Phone contacts
    When we prepare contacts without tags
    And we request collie to create contacts
    And we request collie to delete "YA-1" contact
    Then response is ok

  Scenario: Get brief contact information if user has Phone contacts 
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we request collie to get brief contact information
    Then response is ok with body equal to one at "carddav_propfind_response"

  Scenario: Get empty detailed contact information if the user does not have contacts
    When we prepare contacts without tags
    And we request collie to create contacts
    And we request collie to get detailed contact information
    Then response is ok with body equal to one at "carddav_empty_multiget_response"

  Scenario: Get empty detailed contact information if the user does not have Phone contacts
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create "user" tag "named_tag" for last created contact
    And we request collie to get detailed contact information
    Then response is ok with body equal to one at "carddav_empty_multiget_response"

  Scenario: Get detailed contact information if user has Phone contacts
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we expect request to_vcard
    And we request collie to get detailed contact information
    Then response is ok with body equal to one at "carddav_multiget_response"

  Scenario: Create contact successfully
    When we expect request from_vcard
    And we request carddav_put to create contact with "kitty.vcf"
    Then response is ok with body equal to one at "carddav_put_successful_create_response"

  Scenario: Create contact if it already existen
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we expect request from_vcard
    And we request carddav_put to create contact with "YA-1"
    Then response is ok with body equal to one at "carddav_put_alredy_exists_create_response"

  Scenario: Update contact if it not existen
    When we expect request from_vcard
    And we request carddav_put to update contact with "kitty.vcf" and etag "3-5"
    Then response is ok with body equal to one at "carddav_put_uri_not_found_update_response"

  Scenario: Update contact if its etag not valid
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we expect request from_vcard
    And we request carddav_put to update contact with "YA-1" and etag "3-5"
    Then response is ok with body equal to one at "carddav_put_etag_mismatch_update_response"

  Scenario: Update contact successfully
    When we prepare contacts without tags
    And we request collie to create contacts
    And we create system tag "Phone" for last created contact
    And we expect request from_vcard
    And we request carddav_put to update contact with "YA-1" and etag "1-2"
    Then response is ok with body equal to one at "carddav_put_successful_update_response"
