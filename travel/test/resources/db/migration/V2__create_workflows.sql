create table workflows (
  id UUID,
  supervisor_id UUID,
  entity_id UUID,
  entity_type varchar(256),

  state int,
  version int,
  workflow_version int,

  created_at timestamp,
  updated_at timestamp,

  sleep_till timestamp,

  processing_pool_id int,

  primary key(id)
);
