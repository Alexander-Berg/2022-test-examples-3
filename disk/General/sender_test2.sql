CREATE DATABASE IF NOT EXISTS sender_test2;

CREATE TABLE IF NOT EXISTS sender_test2.counter_v2_part
(
    email String, 
    date Date, 
    campaign UInt64, 
    account UInt64, 
    sign Int8
) ENGINE = ReplicatedSummingMergeTree('/clickhouse/tables/{shard}/sender_test2_counter_v2_part', '{replica}', date, (date, email, campaign, account), 8192, sign);

CREATE TABLE IF NOT EXISTS sender_test2.counter_v2 AS sender_test2.counter_v2_part ENGINE = Distributed(olapdb, sender_test2, counter_v2_part, sipHash64(email));

CREATE TABLE IF NOT EXISTS sender_test2.counter_part
(
    email String,
    date Date,
    campaign UInt64,
    account UInt64,
    sign Int8
)
ENGINE = ReplicatedCollapsingMergeTree('/clickhouse/tables/{shard}/sender_test2_counter_part', '{replica}', date, (date, email, campaign), 8192, sign);

CREATE TABLE IF NOT EXISTS sender_test2.counter AS sender_test2.counter_part ENGINE = Distributed(olapdb, sender_test2, counter_part, sipHash64(email));

CREATE TABLE IF NOT EXISTS sender_test2.delivery_activity_part (
    date Date,
    channel String,
    recepient String,
    event_date DateTime,
    account String,
    campaign UInt64,
    letter UInt64,
    letter_code String,
    message_id String,
    status UInt64,
    test_letter UInt8
) Engine = ReplicatedMergeTree('/clickhouse/tables/{shard}/sender_test2_delivery_activity_part', '{replica}', date, (channel, event_date, campaign, recepient, letter), 8192);

CREATE TABLE IF NOT EXISTS sender_test2.delivery_activity AS sender.delivery_activity_part ENGINE = Distributed(olapdb, sender_test2, delivery_activity_part, sipHash64(recepient));

CREATE TABLE IF NOT EXISTS sender_test2.click_activity_part (
    date Date,
    channel String,
    event String,
    event_type String,
    recepient String,
    event_date DateTime,
    campaign UInt64,
    account String,
    letter UInt64,
    link_id String,
    link_url String,
    test_letter UInt8,
    message_id String
) Engine = ReplicatedMergeTree('/clickhouse/tables/{shard}/sender_test2_click_activity_part', '{replica}', date, (channel, event_date, campaign, recepient, letter, link_id), 8192);

CREATE TABLE IF NOT EXISTS sender_test2.click_activity AS sender.click_activity_part ENGINE = Distributed(olapdb, sender_test2, click_activity_part, sipHash64(recepient));
