Feature: Find a job for Doberman

  Scenario: Find job when there are no jobs
    Given empty doberman jobs
    When some doberman try find job
    Then he got NULL job


  Scenario: Find new job (when job exists, but with NULL heartbeated)
    Given only "BBS" in doberman jobs
       """
          launch_id: null
          hostname: null
          assigned: null
          heartbeated: null
       """
    When some doberman try find job
    Then he got "BBS" job


  Scenario: Find job with expired heartbeated
    Given only "cloud" in doberman jobs
        """
          launch_id: bare-metal-dobby
          hostname: attach-load.mail.yandex.net
          assigned: 2016-11-21T18:33:01.1+03:00
          heartbeated: 2017-07-27T16:15:21.1+03:00
          timeout: !TimeDelta {seconds: 60}
        """
    When some doberman try find job
    Then he got "cloud" job


  Scenario: Find job do not touch special jobs (aka special worker_id)
    Given only "husky" in doberman jobs
    When some doberman try find job
    Then he got NULL job


  Scenario: Find a job updates doberman jobs
    Given only "BBS" in doberman jobs
    When some doberman try find job
        """
          i_launch_id: DobbyWorker
          i_hostname: test-dobby-at.ya.ru
          i_worker_version: 0.1-test
        """
    Then doberman jobs are
        """
         - worker_id: BBS
           launch_id: DobbyWorker
           hostname: test-dobby-at.ya.ru
           worker_version: 0.1-test
        """


  Scenario: Find a job update heartbeated and assigned
    Given only "BBS" in doberman jobs
    When some doberman try find job
    Then "BBS" job has recent heartbeated
    And "BBS" job has recent assigned


  Scenario: Find a job writes to doberman_jobs_change_log
    Given only "Logbrocker" in doberman jobs
       """
          launch_id: bare-metal-dobby
          hostname: attach-load.mail.yandex.net
          assigned: 2016-11-21T18:33:01.1+03:00
          heartbeated: 2017-07-27T16:15:21.1+03:00
          timeout: !TimeDelta {seconds: 60}
       """
    When some doberman try find job
    Then he got "Logbrocker" job
    And last record in doberman changelog has "Logbrocker" worker with info
        """
          worker_id: Logbrocker
          launch_id: bare-metal-dobby
          hostname: attach-load.mail.yandex.net
          # not a timestamps cause "info" is jsob
          assigned: '2016-11-21T18:33:01.1+03:00'
          heartbeated: '2017-07-27T16:15:21.1+03:00'
        """


  Scenario: Two dobermans try find jobs
    Given only "BBS" in doberman jobs
    When "FirstDobby" try find job
    Then he got "BBS" job
    When "SecondDobby" try find job
    Then he got NULL job
    And doberman jobs are
        """
        - worker_id: BBS
          launch_id: FirstDobby
        """


  Scenario: Two dobermans try find jobs concurrently
    Given only "BBS" in doberman jobs
    When "FirstDobby" try find job as "$first"
    And "SecondDobby" try find job as "$second"
    And we rollback "$first"
    And we commit "$second"
    Then doberman jobs are
        """
        - worker_id: BBS
          launch_id: SecondDobby
        """


  Scenario: Confirm doberman job
    Given only "BBS" in doberman jobs
        """
          launch_id: TestDobby
          assigned: 2016-11-21T18:33:01.10+03:00
          heartbeated: 2017-07-27T16:15:21.1+03:00
          timeout: !TimeDelta {seconds: 60}
        """
    When "TestDobby" try confirm "BBS" job
    Then he got confirmed "yes"
    And "BBS" job has recent heartbeated


  Scenario: Confirm lost doberman job
    Given only "BBS" in doberman jobs
        """
          launch_id: AnotherWorkingDobby
          heartbeated: 2017-07-27 16:15:21.10 +3
          timeout: !TimeDelta {seconds: 60}
        """
    When "TestDobby" try confirm "BBS" job
    Then he got confirmed "no"


  @concurrent
  Scenario: Concurrent find with confirm
    Given only "mail" in doberman jobs
        """
          launch_id: SlowDobby
          heartbeated: 1970-01-01 16:15:21.10 +3
          timeout: !TimeDelta {seconds: 60}
        """
    When "FastDobby" try find job as "$fast-find"
    And "SlowDobby" try confirm "mail" job as "$slow-confirm"
    When we commit "$fast-find"
    Then he got "mail" job
    When we commit "$slow-confirm"
    Then he got confirmed "no"
    Then doberman jobs are
        """
        - worker_id: mail
          launch_id: FastDobby
        """

  Scenario: Confirm non existent job
    Given only "mail" in doberman jobs
    When "BuggyDobby" try confirm "non-existent-job" job as "$confirm"
    Then commit "$confirm" should produce "NonExistentDobermanJob"
