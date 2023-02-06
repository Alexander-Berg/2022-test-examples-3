DROP TABLE IF EXISTS riverbank_testing.music_generative_music_generative_backend_log;
CREATE TABLE riverbank_testing.music_generative_music_generative_backend_log
(
    timestamp  Float64,
    session_id Nullable(String),
    request    String,
    request_body Nullable(String),
    error Nullable(String),
    data Nullable(String),
    vsid Nullable(String),
    request_id Nullable(String),
    _rest Nullable(String),
    _timestamp DateTime,
    _partition String,
    _offset    UInt64,
    _idx       UInt32
) ENGINE = Log();
