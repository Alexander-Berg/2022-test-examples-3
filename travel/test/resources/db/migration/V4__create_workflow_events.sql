create table workflow_events (
  id bigint,
  workflow_id UUID,
  created_at timestamp,
  processed_at timestamp,
  class_name varchar(512),
  data bytea,
  state int,
  times_tried integer default 0,

  primary key(id)
);

create sequence workflow_events_id_seq start with 1000;
