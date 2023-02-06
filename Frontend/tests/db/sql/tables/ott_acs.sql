DROP TABLE IF EXISTS riverbank_testing._ott_logs_access_log_d;
CREATE TABLE riverbank_testing._ott_logs_access_log_d (
  timestamp UInt64,
  _logfeller_timestamp UInt64,
  _timestamp DateTime('Europe/Moscow'),
  _partition String,
  _offset UInt64,
  _idx UInt32,
  iso_eventtime String,
  queueId FixedString(36),
  application String,
  environment String,
  instance String,
  protocol String,
  method String,
  request String,
  status Int32,
  ip String,
  user_agent String,
  response_size Int64,
  duration Int64,
  thread String,
  requestId String,
  _rest String,
  component String,
  x_forwarded_for String,
  datacenter String
) ENGINE = MergeTree()
ORDER BY timestamp;
