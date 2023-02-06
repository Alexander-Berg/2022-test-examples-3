
-- name: create_folder
select fid from code.create_folder(
    :uid, :name, :parent_fid, 'user'
);

-- name: add_shared_folder
select * from code.add_folder_to_shared_folders(
    :uid, :fid
);

--name: remove_subscription
DELETE FROM
    mail.shared_folder_subscriptions
WHERE
    uid=:uid AND
    fid=:fid
;

--name: add_to_synced_messages
INSERT INTO mail.synced_messages
    (uid, mid,
     tid, revision,
     subscription_id, owner_mid,
     owner_tid, owner_revision)
VALUES
    (:uid, :mid,
     :mid, 1,
     :subscription_id, :mid,
     :mid, 1);

--name: terminate_subscription
UPDATE
    mail.shared_folder_subscriptions
SET
    state='terminated'
WHERE
    uid=:uid AND
    subscription_id=:subscription_id;

--name: add_subscription
SELECT * FROM code.add_subscriber_to_shared_folders(
    :owner_uid, :owner_fid, :subscriber_uid
);

--name: add_unsubscribe_task
SELECT * FROM code.add_unsubscribe_task(
    :task_request_id, :owner_uid, :owner_fids, :subscriber_uid, :root_subscriber_fid
);
