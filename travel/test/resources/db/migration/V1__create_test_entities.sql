create table test_entities
(
  id          UUID,
  workflow_id UUID,
  state       VARCHAR(128),
  last_transition_at timestamp,
  version     int,
  primary key(id)
);
