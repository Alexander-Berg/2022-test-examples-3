CREATE TABLE table1 (
    id              BIGINT(20),
    id2             BIGINT(20),
    long_val        BIGINT,
    ulong_val       BIGINT(20) unsigned,
    ulong_hash      BIGINT UNSIGNED,
    int_val         INTEGER,
    uint_val        INTEGER UNSIGNED,
    PRIMARY KEY `id` (`id`),
    KEY         `id2` (`id2`)
)
