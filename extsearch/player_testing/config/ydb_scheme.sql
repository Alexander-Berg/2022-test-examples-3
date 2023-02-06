CREATE TABLE workers(
    fqdn String,
    last_access Timestamp,
    state String,
    stream_id String,
    PRIMARY KEY(fqdn)
);

CREATE TABLE session(
    sid String,
    fqdn String,
    created Timestamp,
    last_access Timestamp,
    PRIMARY KEY(sid)
);
