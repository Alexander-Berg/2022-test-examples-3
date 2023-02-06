insert into `shard_client_id` values
    (123, 4),
    (124, 5),
    (444, 4),
    (555, 5),
    (777, 2);

insert into `shard_uid` values
    (17179869184, 123),
    (17179869185, 123),
    (17179869186, 124),
    (123456789, 444),
    (234567890, 444),
    (345678901, 777);

insert into `shard_login` values
    ('foo', 17179869184),
    ('bar', 17179869185),
    ('foobar', 17179869186),
    ('special1', 123456789),
    ('special2', 234567890),
    ('NonLowerCase', 345678901);
