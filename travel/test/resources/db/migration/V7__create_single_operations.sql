create table single_operations (
  id          UUID,
  workflow_id UUID,
  state       int,
  last_transition_at timestamp,
  version     int,

  unique_name varchar,
  name varchar,
  operation_type varchar,
  input other,
  output other,

  scheduled_at timestamp,
  commit_sent boolean,

  created_at timestamp,
  updated_at timestamp,

  primary key(id),
  unique(unique_name)
);
