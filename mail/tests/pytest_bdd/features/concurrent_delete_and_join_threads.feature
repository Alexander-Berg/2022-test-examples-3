Feature: Concurrent delete message with join threads

  @data-bug @MAILPG-1123
  Scenario: Concurrent join threads and thread delete store message with references cause FK threads_hashes -> threads
    Given new initialized user
    When we store into "inbox"
      | mid | tid | rule       |
      | $1  | 1   | references |
      | $2  | 2   | references |
    And we try delete "$1" as "$del_op"
    And we try join "2" into "1" as "$join_op"
    When we commit "$del_op"
    Then commit "$join_op" should produce "InvalidTIDsError"
    And check produce nothing
