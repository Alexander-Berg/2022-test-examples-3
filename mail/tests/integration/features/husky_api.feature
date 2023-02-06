Feature: Tests on husky-api "add task" function

  Scenario: Clone user into unregistered user using "add task" function
    Given new user "Alice"
    And new unregistered user "Bob" with pg db in blackbox
    When we use "add task" to clone "Alice" into "Bob"
    Then there is our "clone_user" task
    And task is successful
    And "Bob" has same mails as "Alice" except st_ids and user state are different

   Scenario: Create task with unknown type
     When we add task with unknown type
     Then response status is error
     And error in response is "unknown task type"
