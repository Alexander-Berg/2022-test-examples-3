DROP TABLE IF EXISTS riverbank_testing.riverbank_session_aggregates;
create table riverbank_testing.riverbank_session_aggregates
(
    date       Date,
    vsid       String,
    ottsession String,
    min_timestamp DateTime('Europe/Moscow'),
    max_timestamp DateTime('Europe/Moscow')
) ENGINE = Log();