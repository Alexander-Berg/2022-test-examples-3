Feature: pg to pg transfer

    Scenario: Transfer change shard_id
        Given new user in first shard
        When we transfer his metadata to second shard
        Then user leave in second shard

    Scenario: Transfer
        Given new user in first shard
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer write transfer change
        Given new user in first shard
        When we transfer his metadata to second shard
        Then in changelog there is "transfer" change

    Scenario: Transfer mark user as not here in source shard
        Given new user in first shard
        When we transfer his metadata to second shard
        Then user marked as not here in first shard
        And contacts user marked as not here in first shard
        And user marked as here in second shard
        And contacts user marked as here in second shard

    Scenario: Transfer with fill_changelog
        Given new user in first shard
        When we transfer his metadata to second shard with
           """
           fill_change_log: true
           """
        Then in changelog there are "reindex" and "transfer" changes

    Scenario: Transfer user with deleted messages into `old` shard
        Given new user with deleted messages in first shard
        When we transfer his metadata to second shard
        Then his metadata is identical
        When we transfer his metadata to first shard
        Then his metadata is identical

    Scenario: Mailish transfer
        Given new mailish user in first shard
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with collectors
        When we have new user with collectors in first shard
        And we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with tabs
        Given new user with tabs in first shard
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with contacts
        Given new user with contacts in first shard
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer notified user
        Given new user
        And he is in "notified" state with "2" notifies
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer archived user 
        Given new user
        And he is in "archived" state with "2" notifies
        And he is in "archivation_complete" archivation state
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with backup settings 
        Given new user
        And folders with types "inbox,sent" and tabs "relevant" are in backup settings
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with backup 
        Given new user
        And folders with types "inbox" are in backup settings
        And user has filled backup
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with restore 
        Given new user
        And folders with types "inbox" are in backup settings
        And user has filled backup
        And user has restore
        When we transfer his metadata to second shard
        Then his metadata is identical

    Scenario: Transfer user with reply later messages
        Given new user in first shard
        And user has folder "reply_later" with symbol "reply_later"
        And user has SO labels
            | lid         | so_type |
            | $news_lid   | 100     |
            | $social_lid | 101     |
        And user has messages in "reply_later"
            | mid | lids      |
            | $1  | $news_lid |
            | $2  |           |
            | $3  |           |
        And user has reply later stickers for messages
            | mid |
            | $1  |
            | $2  |
        When we transfer his metadata to second shard
        Then his metadata is identical
