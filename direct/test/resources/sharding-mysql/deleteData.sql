DELETE FROM shard_client_id
WHERE ClientID in (123, 124, 444, 555, 777);

DELETE FROM shard_uid
WHERE uid IN (17179869184, 17179869185, 17179869186, 123456789, 234567890, 345678901);

DELETE FROM shard_login
WHERE login IN ('foo', 'bar', 'foobar', 'special1', 'special2', 'NonLowerCase');
