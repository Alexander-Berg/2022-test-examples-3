INSERT INTO shard_client_id VALUES
    (123, 4),
    (124, 5),
    (444, 4),
    (555, 5),
    (777, 2);

INSERT INTO shard_uid VALUES
    (17179869184, 123),
    (17179869185, 123),
    (17179869186, 124),
    (123456789, 444),
    (234567890, 444),
    (345678901, 777);

INSERT INTO shard_login VALUES
    ('foo', 17179869184),
    ('bar', 17179869185),
    ('foobar', 17179869186),
    ('special1', 123456789),
    ('special2', 234567890),
    ('NonLowerCase', 345678901);
